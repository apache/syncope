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
package org.apache.syncope.core.sync.impl;

import org.apache.syncope.core.sync.SyncProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.role.RMapping;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.rest.data.SearchCondConverter;
import org.apache.syncope.core.sync.PushActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.EntitlementUtil;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Job for executing synchronization (towards external resource) tasks.
 *
 * @see AbstractSyncJob
 * @see PushTask
 */
public class PushJob extends AbstractSyncJob<PushTask, PushActions> {

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
     * Role DAO.
     */
    @Autowired
    private RoleDAO roleDAO;

    private final int PAGE_SIZE = 1000;

    @Override
    protected String executeWithSecurityContext(
            final PushTask pushTask,
            final SyncPolicySpec syncPolicySpec,
            final Connector connector,
            final UMapping uMapping,
            final RMapping rMapping,
            final boolean dryRun) throws JobExecutionException {
        LOG.debug("Execute synchronization (push) with resource {}", pushTask.getResource());

        final List<SyncResult> results = new ArrayList<SyncResult>();

        final Set<Long> authorizations = EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        final SyncProfile<PushTask, PushActions> profile =
                new SyncProfile<PushTask, PushActions>(connector, pushTask);
        profile.setActions(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(syncPolicySpec.getConflictResolutionAction());
        profile.setResults(results);

        final UserPushResultHandler uhandler =
                (UserPushResultHandler) ((DefaultListableBeanFactory) ApplicationContextProvider.
                getApplicationContext().getBeanFactory()).createBean(
                UserPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        final RolePushResultHandler rhandler =
                (RolePushResultHandler) ((DefaultListableBeanFactory) ApplicationContextProvider.
                getApplicationContext().getBeanFactory()).createBean(
                RolePushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        rhandler.setProfile(profile);

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.beforeAll(profile);
            }
        }

        if (uMapping != null) {
            final int count = userDAO.count(authorizations);
            for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
                final List<SyncopeUser> localUsers = getUsers(authorizations, pushTask, page);

                for (SyncopeUser localUser : localUsers) {
                    try {
                        // user propagation
                        uhandler.handle(localUser);
                    } catch (Exception e) {
                        LOG.warn("Failure pushing user '{}' on '{}'", localUser, pushTask.getResource());
                        if (!continueOnError()) {
                            throw new JobExecutionException("While pushing users on connector", e);
                        }
                    }
                }
            }
        }

        if (rMapping != null) {
            final List<SyncopeRole> localRoles = getRoles(authorizations, pushTask, PAGE_SIZE);

            for (SyncopeRole localRole : localRoles) {
                try {
                    // role propagation
                    rhandler.handle(localRole);
                } catch (Exception e) {
                    LOG.warn("Failure pushing role '{}' on '{}'", localRole, pushTask.getResource());
                    if (!continueOnError()) {
                        throw new JobExecutionException("While pushing roles on connector", e);
                    }
                }
            }
        }

        if (!profile.isDryRun()) {
            for (PushActions action : actions) {
                action.afterAll(profile, results);
            }
        }

        final String result = createReport(results, pushTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result;
    }

    protected boolean continueOnError() {
        return true;
    }

    private List<SyncopeUser> getUsers(final Set<Long> authorizations, final PushTask pushTask, final int page) {
        final String filter = pushTask.getUserFilter();
        if (StringUtils.isBlank(filter)) {
            return userDAO.findAll(authorizations, page, PAGE_SIZE);
        } else {
            return searchDAO.<SyncopeUser>search(
                    authorizations, SearchCondConverter.convert(filter),
                    Collections.<OrderByClause>emptyList(), SubjectType.USER);
        }
    }

    private List<SyncopeRole> getRoles(final Set<Long> authorizations, final PushTask pushTask, final int page) {
        final String filter = pushTask.getRoleFilter();
        if (StringUtils.isBlank(filter)) {
            return roleDAO.findAll();
        } else {
            return searchDAO.<SyncopeRole>search(
                    authorizations, SearchCondConverter.convert(filter),
                    Collections.<OrderByClause>emptyList(), SubjectType.ROLE);
        }
    }
}
