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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public class PushJobDelegate extends AbstractProvisioningJobDelegate<PushTask> {

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Search DAO.
     */
    @Autowired
    private AnySearchDAO searchDAO;

    /**
     * Group DAO.
     */
    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private RealmDAO realmDAO;

    private AnyDAO<?> getAnyDAO(final AnyTypeKind anyTypeKind) {
        AnyDAO<?> result;
        switch (anyTypeKind) {
            case USER:
                result = userDAO;
                break;

            case GROUP:
                result = groupDAO;
                break;

            case ANY_OBJECT:
            default:
                result = anyObjectDAO;
        }

        return result;
    }

    private void doHandle(
            final List<? extends Any<?>> anys,
            final SyncopePushResultHandler handler,
            final ExternalResource resource)
            throws JobExecutionException {

        for (Any<?> any : anys) {
            try {
                handler.handle(any.getKey());
            } catch (Exception e) {
                LOG.warn("Failure pushing '{}' on '{}'", any, resource, e);
                throw new JobExecutionException("While pushing " + any + " on " + resource, e);
            }
        }
    }

    @Override
    protected String doExecuteProvisioning(
            final PushTask pushTask,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException {

        LOG.debug("Executing push on {}", pushTask.getResource());

        List<PushActions> actions = new ArrayList<>();
        pushTask.getActionsClassNames().forEach(className -> {
            try {
                Class<?> actionsClass = Class.forName(className);

                PushActions pushActions = (PushActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                actions.add(pushActions);
            } catch (Exception e) {
                LOG.info("Class '{}' not found", className, e);
            }
        });

        ProvisioningProfile<PushTask, PushActions> profile = new ProvisioningProfile<>(connector, pushTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(null);

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.beforeAll(profile);
            }
        }

        // First OrgUnits...
        if (pushTask.getResource().getOrgUnit() != null) {
            SyncopePushResultHandler rhandler = (SyncopePushResultHandler) ApplicationContextProvider.getBeanFactory().
                    createBean(RealmPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
            rhandler.setProfile(profile);

            for (Realm realm : realmDAO.findDescendants(profile.getTask().getSourceRealm())) {
                // Never push the root realm
                if (realm.getParent() != null) {
                    try {
                        rhandler.handle(realm.getKey());
                    } catch (Exception e) {
                        LOG.warn("Failure pushing '{}' on '{}'", realm, pushTask.getResource(), e);
                        throw new JobExecutionException("While pushing " + realm + " on " + pushTask.getResource(), e);
                    }
                }
            }
        }

        // ...then provisions for any types
        AnyObjectPushResultHandler ahandler = (AnyObjectPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(AnyObjectPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ahandler.setProfile(profile);

        UserPushResultHandler uhandler =
                (UserPushResultHandler) ApplicationContextProvider.getBeanFactory().
                        createBean(UserPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        GroupPushResultHandler ghandler =
                (GroupPushResultHandler) ApplicationContextProvider.getBeanFactory().
                        createBean(GroupPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ghandler.setProfile(profile);

        for (Provision provision : pushTask.getResource().getProvisions()) {
            if (provision.getMapping() != null) {
                AnyDAO<?> anyDAO = getAnyDAO(provision.getAnyType().getKind());

                SyncopePushResultHandler handler;
                switch (provision.getAnyType().getKind()) {
                    case USER:
                        handler = uhandler;
                        break;

                    case GROUP:
                        handler = ghandler;
                        break;

                    case ANY_OBJECT:
                    default:
                        handler = ahandler;
                }

                Optional<? extends PushTaskAnyFilter> anyFilter = pushTask.getFilter(provision.getAnyType());
                String filter = anyFilter.isPresent()
                        ? anyFilter.get().getFIQLCond()
                        : null;
                SearchCond cond = StringUtils.isBlank(filter)
                        ? anyDAO.getAllMatchingCond()
                        : SearchCondConverter.convert(filter);
                int count = searchDAO.count(
                        Collections.singleton(profile.getTask().getSourceRealm().getFullPath()),
                        cond,
                        provision.getAnyType().getKind());
                for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    List<? extends Any<?>> anys = searchDAO.search(
                            Collections.singleton(profile.getTask().getSourceRealm().getFullPath()),
                            cond,
                            page,
                            AnyDAO.DEFAULT_PAGE_SIZE,
                            Collections.<OrderByClause>emptyList(),
                            provision.getAnyType().getKind());
                    doHandle(anys, handler, pushTask.getResource());
                }
            }
        }

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.afterAll(profile);
            }
        }

        String result = createReport(profile.getResults(), pushTask.getResource(), dryRun);
        LOG.debug("Push result: {}", result);
        return result;
    }
}
