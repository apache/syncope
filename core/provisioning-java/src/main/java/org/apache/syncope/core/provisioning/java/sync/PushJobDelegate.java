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
package org.apache.syncope.core.provisioning.java.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.provisioning.api.sync.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.UserPushResultHandler;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public class PushJobDelegate extends AbstractProvisioningJobDelegate<PushTask> {

    private static final int PAGE_SIZE = 1000;

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

    protected void handle(
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
        for (String className : pushTask.getActionsClassNames()) {
            try {
                Class<?> actionsClass = Class.forName(className);

                PushActions syncActions = (PushActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                actions.add(syncActions);
            } catch (Exception e) {
                LOG.info("Class '{}' not found", className, e);
            }
        }

        ProvisioningProfile<PushTask, PushActions> profile = new ProvisioningProfile<>(connector, pushTask);
        profile.setDryRun(dryRun);
        profile.setResAct(null);

        AnyObjectPushResultHandler ahandler =
                (AnyObjectPushResultHandler) ApplicationContextProvider.getBeanFactory().
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

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.beforeAll(profile);
            }
        }

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

                String filter = pushTask.getFilter(provision.getAnyType()) == null
                        ? null
                        : pushTask.getFilter(provision.getAnyType()).getFIQLCond();
                if (StringUtils.isBlank(filter)) {
                    handle(anyDAO.findAll(), handler, pushTask.getResource());
                } else {
                    int count = anyDAO.count(SyncopeConstants.FULL_ADMIN_REALMS);
                    for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
                        List<? extends Any<?>> anys = searchDAO.search(
                                SyncopeConstants.FULL_ADMIN_REALMS,
                                SearchCondConverter.convert(filter),
                                page,
                                PAGE_SIZE,
                                Collections.<OrderByClause>emptyList(),
                                provision.getAnyType().getKind());
                        handle(anys, handler, pushTask.getResource());
                    }
                }
            }
        }

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.afterAll(profile);
            }
        }

        String result = createReport(profile.getResults(), pushTask.getResource().getSyncTraceLevel(), dryRun);
        LOG.debug("Sync result: {}", result);
        return result;
    }
}
