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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class PushJobDelegate extends AbstractProvisioningJobDelegate<PushTask> implements SyncopePushExecutor {

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected SearchCondVisitor searchCondVisitor;

    protected ProvisioningProfile<PushTask, PushActions> profile;

    protected final Map<String, MutablePair<Integer, String>> handled = new ConcurrentHashMap<>();

    protected final Map<String, PushActions> perContextActions = new ConcurrentHashMap<>();

    @Override
    public void reportHandled(final String anyType, final String key) {
        MutablePair<Integer, String> pair = handled.get(anyType);
        if (pair == null) {
            pair = MutablePair.of(0, null);
            handled.put(anyType, pair);
        }
        pair.setLeft(pair.getLeft() + 1);
        pair.setRight(key);

        if (!handled.isEmpty()) {
            StringBuilder builder = new StringBuilder("Processed:\n");
            handled.forEach((k, v) -> builder.append(' ').append(v.getLeft()).append('\t').
                    append(k).
                    append(" / latest: ").append(v.getRight()).
                    append('\n'));
            setStatus(builder.toString());
        }
    }

    @Override
    public boolean wasInterruptRequested() {
        return interrupt;
    }

    @Override
    public void setInterrupted() {
        this.interrupted = true;
    }

    protected boolean doHandle(
            final List<? extends Any<?>> anys,
            final PushResultHandlerDispatcher dispatcher,
            final ExternalResource resource)
            throws JobExecutionException {

        boolean result = true;
        for (int i = 0; i < anys.size() && result; i++) {
            try {
                result = dispatcher.handle(anys.get(i).getType().getKey(), anys.get(i).getKey());
            } catch (Exception e) {
                LOG.warn("Failure pushing '{}' on '{}'", anys.get(i), resource, e);
                throw new JobExecutionException("While pushing " + anys.get(i) + " on " + resource, e);
            }
        }
        return result;
    }

    protected RealmPushResultHandler buildRealmHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultRealmPushResultHandler.class);
    }

    protected AnyObjectPushResultHandler buildAnyObjectHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultAnyObjectPushResultHandler.class);
    }

    protected UserPushResultHandler buildUserHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultUserPushResultHandler.class);
    }

    protected GroupPushResultHandler buildGroupHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultGroupPushResultHandler.class);
    }

    protected List<PushActions> getPushActions(final List<? extends Implementation> impls) {
        List<PushActions> result = new ArrayList<>();

        impls.forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return result;
    }

    @Override
    protected String doExecuteProvisioning(
            final PushTask pushTask,
            final Connector connector,
            final boolean dryRun,
            final String executor,
            final JobExecutionContext context) throws JobExecutionException {

        LOG.debug("Executing push on {}", pushTask.getResource());

        profile = new ProvisioningProfile<>(connector, pushTask);
        profile.getActions().addAll(getPushActions(pushTask.getActions()));
        profile.setDryRun(dryRun);
        profile.setConflictResolutionAction(
                Optional.ofNullable(pushTask.getResource().getPushPolicy()).
                        map(PushPolicy::getConflictResolutionAction).
                        orElse(ConflictResolutionAction.IGNORE));
        profile.setExecutor(executor);

        PushResultHandlerDispatcher dispatcher = new PushResultHandlerDispatcher(profile, this);

        if (!profile.isDryRun()) {
            for (PushActions action : profile.getActions()) {
                action.beforeAll(profile);
            }
        }

        setStatus("Initialization completed");

        // First realms...
        if (pushTask.getResource().getOrgUnit() != null) {
            setStatus("Pushing realms");

            dispatcher.addHandlerSupplier(SyncopeConstants.REALM_ANYTYPE, () -> {
                RealmPushResultHandler handler = buildRealmHandler();
                handler.setProfile(profile);
                return handler;
            });

            // Never push the root realm
            List<Realm> realms = realmDAO.findDescendants(
                    profile.getTask().getSourceRealm().getFullPath(), null, -1, -1).stream().
                    filter(realm -> realm.getParent() != null).collect(Collectors.toList());
            boolean result = true;
            for (int i = 0; i < realms.size() && result; i++) {
                try {
                    result = dispatcher.handle(SyncopeConstants.REALM_ANYTYPE, realms.get(i).getKey());
                } catch (Exception e) {
                    LOG.warn("Failure pushing '{}' on '{}'", realms.get(i), pushTask.getResource(), e);
                    throw new JobExecutionException(
                            "While pushing " + realms.get(i) + " on " + pushTask.getResource(), e);
                }
            }
        }

        // ...then provisions for any types
        ProvisionSorter provisionSorter = getProvisionSorter(pushTask);

        for (Provision provision : pushTask.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).sorted(provisionSorter).
                collect(Collectors.toList())) {

            setStatus("Pushing " + provision.getAnyType());

            AnyType anyType = anyTypeDAO.findById(provision.getAnyType()).
                    orElseThrow(() -> new NotFoundException("AnyType" + provision.getAnyType()));

            AnyDAO<?> anyDAO = anyUtilsFactory.getInstance(anyType.getKind()).dao();

            dispatcher.addHandlerSupplier(provision.getAnyType(), () -> {
                SyncopePushResultHandler handler;
                switch (anyType.getKind()) {
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
                return handler;
            });

            String filter = pushTask.getFilter(anyType.getKey()).orElse(null);
            SearchCond cond = StringUtils.isBlank(filter)
                    ? anyDAO.getAllMatchingCond()
                    : SearchCondConverter.convert(searchCondVisitor, filter);
            int count = searchDAO.count(
                    profile.getTask().getSourceRealm(),
                    true,
                    Set.of(profile.getTask().getSourceRealm().getFullPath()),
                    cond,
                    anyType.getKind());
            boolean result = true;
            for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1 && result; page++) {
                List<? extends Any<?>> anys = searchDAO.search(
                        profile.getTask().getSourceRealm(),
                        true,
                        Set.of(profile.getTask().getSourceRealm().getFullPath()),
                        cond,
                        page,
                        AnyDAO.DEFAULT_PAGE_SIZE,
                        List.of(),
                        anyType.getKind());
                result = doHandle(anys, dispatcher, pushTask.getResource());
            }
        }

        dispatcher.shutdown();

        if (!profile.isDryRun()) {
            for (PushActions action : profile.getActions()) {
                action.afterAll(profile);
            }
        }

        setStatus("Push done");

        String result = createReport(profile.getResults(), pushTask.getResource(), dryRun);
        LOG.debug("Push result: {}", result);
        return result;
    }
}
