/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.spring.ImplementationManager;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public class PushJobDelegate extends AbstractProvisioningJobDelegate<PushTask> {

    /**
     * Search DAO.
     */
    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected SearchCondVisitor searchCondVisitor;

    protected ProvisioningProfile<PushTask, PushActions> profile;

    protected final Map<String, MutablePair<Integer, String>> handled = new HashMap<>();

    protected void reportHandled(final String anyType, final String key) {
        MutablePair<Integer, String> pair = handled.get(anyType);
        if (pair == null) {
            pair = MutablePair.of(0, null);
            handled.put(anyType, pair);
        }
        pair.setLeft(pair.getLeft() + 1);
        pair.setRight(key);
    }

    @Override
    public String currentStatus() {
        synchronized (status) {
            if (!handled.isEmpty()) {
                StringBuilder builder = new StringBuilder("Processed:\n");
                handled.forEach((key, value) -> builder.append(' ').append(value.getLeft()).append('\t').
                        append(key).
                        append(" / latest: ").append(value.getRight()).
                        append('\n'));
                status.set(builder.toString());
            }
        }
        return status.get();
    }

    protected void doHandle(
            final List<? extends Any<?>> anys,
            final SyncopePushResultHandler handler,
            final ExternalResource resource)
            throws JobExecutionException {

        for (int i = 0; i < anys.size() && !interrupt; i++) {
            try {
                handler.handle(anys.get(i).getKey());
                reportHandled(
                        anys.get(i).getType().getKey(),
                        (anys.get(i) instanceof User
                        ? ((User) anys.get(i)).getUsername()
                        : anys.get(i) instanceof Group
                        ? ((Group) anys.get(i)).getName()
                        : ((AnyObject) anys.get(i)).getName()));
            } catch (Exception e) {
                LOG.warn("Failure pushing '{}' on '{}'", anys.get(i), resource, e);
                throw new JobExecutionException("While pushing " + anys.get(i) + " on " + resource, e);
            }
        }
    }

    protected RealmPushResultHandler buildRealmHandler() {
        return (RealmPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultRealmPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    protected AnyObjectPushResultHandler buildAnyObjectHandler() {
        return (AnyObjectPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultAnyObjectPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    protected UserPushResultHandler buildUserHandler() {
        return (UserPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultUserPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    protected GroupPushResultHandler buildGroupHandler() {
        return (GroupPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultGroupPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    @Override
    protected String doExecuteProvisioning(
            final PushTask pushTask,
            final Connector connector,
            final boolean dryRun,
            final String executor,
            final JobExecutionContext context) throws JobExecutionException {

        LOG.debug("Executing push on {}", pushTask.getResource());

        List<PushActions> actions = new ArrayList<>();
        pushTask.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        profile = new ProvisioningProfile<>(connector, pushTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setConflictResolutionAction(pushTask.getResource().getPushPolicy() == null
                ? ConflictResolutionAction.IGNORE
                : pushTask.getResource().getPushPolicy().getConflictResolutionAction());
        profile.setExecutor(executor);

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.beforeAll(profile);
            }
        }

        status.set("Initialization completed");

        // First realms...
        if (pushTask.getResource().getOrgUnit() != null) {
            status.set("Pushing realms");

            RealmPushResultHandler handler = buildRealmHandler();
            handler.setProfile(profile);

            for (Realm realm : realmDAO.findDescendants(profile.getTask().getSourceRealm())) {
                // Never push the root realm
                if (realm.getParent() != null) {
                    try {
                        handler.handle(realm.getKey());
                        reportHandled(SyncopeConstants.REALM_ANYTYPE, realm.getName());
                    } catch (Exception e) {
                        LOG.warn("Failure pushing '{}' on '{}'", realm, pushTask.getResource(), e);
                        throw new JobExecutionException("While pushing " + realm + " on " + pushTask.getResource(), e);
                    }
                }
            }
        }

        // ...then provisions for any types
        ProvisionSorter provisionSorter = new DefaultProvisionSorter();
        if (pushTask.getResource().getProvisionSorter() != null) {
            try {
                provisionSorter = ImplementationManager.build(pushTask.getResource().getProvisionSorter());
            } catch (Exception e) {
                LOG.error("While building {}", pushTask.getResource().getProvisionSorter(), e);
            }
        }

        for (Provision provision : pushTask.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).sorted(provisionSorter).
                collect(Collectors.toList())) {

            status.set("Pushing " + provision.getAnyType().getKey());

            AnyDAO<?> anyDAO = anyUtilsFactory.getInstance(provision.getAnyType().getKind()).dao();

            SyncopePushResultHandler handler;
            switch (provision.getAnyType().getKind()) {
                case USER:
                    handler = buildUserHandler();
                    break;

                case GROUP:
                    handler = buildGroupHandler();
                    break;

                case ANY_OBJECT:
                default:
                    handler = buildAnyObjectHandler();
            }
            handler.setProfile(profile);

            Optional<? extends PushTaskAnyFilter> anyFilter = pushTask.getFilter(provision.getAnyType());
            String filter = anyFilter.map(PushTaskAnyFilter::getFIQLCond).orElse(null);
            SearchCond cond = StringUtils.isBlank(filter)
                    ? anyDAO.getAllMatchingCond()
                    : SearchCondConverter.convert(searchCondVisitor, filter);
            int count = searchDAO.count(
                    Set.of(profile.getTask().getSourceRealm().getFullPath()),
                    cond,
                    provision.getAnyType().getKind());
            for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1 && !interrupt; page++) {
                List<? extends Any<?>> anys = searchDAO.search(
                        Set.of(profile.getTask().getSourceRealm().getFullPath()),
                        cond,
                        page,
                        AnyDAO.DEFAULT_PAGE_SIZE,
                        List.of(),
                        provision.getAnyType().getKind());
                doHandle(anys, handler, pushTask.getResource());
            }
        }

        if (!profile.isDryRun() && !interrupt) {
            for (PushActions action : actions) {
                action.afterAll(profile);
            }
        }

        if (interrupt) {
            interrupted = true;
        }

        status.set("Push done");

        String result = createReport(profile.getResults(), pushTask.getResource(), dryRun);
        LOG.debug("Push result: {}", result);
        return result;
    }
}
