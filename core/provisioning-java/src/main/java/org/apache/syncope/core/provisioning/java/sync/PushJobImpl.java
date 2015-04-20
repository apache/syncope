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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.group.GMapping;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.user.UMapping;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.provisioning.api.job.PushJob;
import org.apache.syncope.core.provisioning.api.sync.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.sync.UserPushResultHandler;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Job for executing synchronization (towards external resource) tasks.
 *
 * @see AbstractProvisioningJob
 * @see PushTask
 * @see PushActions
 */
public class PushJobImpl extends AbstractProvisioningJob<PushTask, PushActions> implements PushJob {

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Search DAO.
     */
    @Autowired
    private SubjectSearchDAO searchDAO;

    /**
     * Group DAO.
     */
    @Autowired
    private GroupDAO groupDAO;

    private final int PAGE_SIZE = 1000;

    @Override
    protected String executeWithSecurityContext(
            final PushTask pushTask,
            final Connector connector,
            final UMapping uMapping,
            final GMapping rMapping,
            final boolean dryRun) throws JobExecutionException {
        LOG.debug("Execute synchronization (push) with resource {}", pushTask.getResource());

        final ProvisioningProfile<PushTask, PushActions> profile = new ProvisioningProfile<>(connector, pushTask);
        if (actions != null) {
            profile.getActions().addAll(actions);
        }
        profile.setDryRun(dryRun);
        profile.setResAct(null);

        final UserPushResultHandler uhandler =
                (UserPushResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(UserPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        final GroupPushResultHandler rhandler =
                (GroupPushResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(GroupPushResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        rhandler.setProfile(profile);

        if (actions != null && !profile.isDryRun()) {
            for (PushActions action : actions) {
                action.beforeAll(profile);
            }
        }

        if (uMapping != null) {
            final int count = userDAO.count(SyncopeConstants.FULL_ADMIN_REALMS);
            for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
                final List<User> localUsers = StringUtils.isBlank(pushTask.getUserFilter())
                        ? userDAO.findAll(SyncopeConstants.FULL_ADMIN_REALMS, page, PAGE_SIZE)
                        : searchDAO.<User>search(SyncopeConstants.FULL_ADMIN_REALMS,
                                SearchCondConverter.convert(pushTask.getUserFilter()),
                                Collections.<OrderByClause>emptyList(), SubjectType.USER);

                for (User localUser : localUsers) {
                    try {
                        // user propagation
                        uhandler.handle(localUser.getKey());
                    } catch (Exception e) {
                        LOG.warn("Failure pushing user '{}' on '{}'", localUser, pushTask.getResource(), e);
                        throw new JobExecutionException("While pushing users on connector", e);
                    }
                }
            }
        }

        if (rMapping != null) {
            final int count = groupDAO.count(SyncopeConstants.FULL_ADMIN_REALMS);
            for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
                final List<Group> localGroups = StringUtils.isBlank(pushTask.getGroupFilter())
                        ? groupDAO.findAll(SyncopeConstants.FULL_ADMIN_REALMS, page, PAGE_SIZE)
                        : searchDAO.<Group>search(SyncopeConstants.FULL_ADMIN_REALMS,
                                SearchCondConverter.convert(pushTask.getGroupFilter()),
                                Collections.<OrderByClause>emptyList(), SubjectType.GROUP);

                for (Group localGroup : localGroups) {
                    try {
                        // group propagation
                        rhandler.handle(localGroup.getKey());
                    } catch (Exception e) {
                        LOG.warn("Failure pushing group '{}' on '{}'", localGroup, pushTask.getResource(), e);
                        throw new JobExecutionException("While pushing groups on connector", e);
                    }
                }
            }
        }

        if (actions != null && !profile.isDryRun()) {
            for (PushActions action : actions) {
                action.afterAll(profile);
            }
        }

        final String result = createReport(profile.getResults(), pushTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result;
    }
}
