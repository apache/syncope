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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.role.RMapping;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.Connector;
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
public class PushJob extends AbstractSyncJob<AbstractSyncopeResultHandler, PushActions> {

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Role DAO.
     */
    @Autowired
    private RoleDAO roleDAO;

    private final int PAGE_SIZE = 1000;

    @Override
    protected String executeWithSecurityContext(final boolean dryRun) throws JobExecutionException {
        if (!(task instanceof PushTask)) {
            throw new JobExecutionException("Task " + taskId + " isn't a PushTask");
        }

        final PushTask pushTask = (PushTask) this.task;

        Connector connector;
        try {
            connector = connFactory.getConnector(pushTask.getResource());
        } catch (Exception e) {
            final String msg = String.format("Connector instance bean for resource %s and connInstance %s not found",
                    pushTask.getResource(), pushTask.getResource().getConnector());

            throw new JobExecutionException(msg, e);
        }

        UMapping uMapping = pushTask.getResource().getUmapping();
        if (uMapping != null && uMapping.getAccountIdItem() == null) {
            throw new JobExecutionException("Invalid user account id mapping for resource " + pushTask.getResource());
        }
        RMapping rMapping = pushTask.getResource().getRmapping();
        if (rMapping != null && rMapping.getAccountIdItem() == null) {
            throw new JobExecutionException("Invalid role account id mapping for resource " + pushTask.getResource());
        }
        if (uMapping == null && rMapping == null) {
            return "No mapping configured for both users and roles: aborting...";
        }

        LOG.debug("Execute synchronization (push) with resource {}", pushTask.getResource());

        final List<SyncResult> results = new ArrayList<SyncResult>();

        final Set<Long> authorizations = EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        // Prepare handler for SyncDelta objects
        final SyncopePushResultHandler handler =
                (SyncopePushResultHandler) ((DefaultListableBeanFactory) ApplicationContextProvider.
                getApplicationContext().getBeanFactory()).createBean(
                SyncopePushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        handler.setConnector(connector);
        handler.setDryRun(dryRun);
        handler.setResults(results);
        handler.setSyncTask(pushTask);
        handler.setActions(actions);

        actions.beforeAll(handler);

        if (uMapping != null) {
            final int count = userDAO.count(authorizations);
            for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
                final List<SyncopeUser> localUsers = userDAO.findAll(authorizations, page, PAGE_SIZE);

                for (SyncopeUser localUser : localUsers) {
                    try {
                        // user propagation
                        handler.handle(localUser);
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
            final List<SyncopeRole> localRoles = roleDAO.findAll();

            for (SyncopeRole localRole : localRoles) {
                try {
                    // role propagation
                    handler.handle(localRole);
                } catch (Exception e) {
                    LOG.warn("Failure pushing role '{}' on '{}'", localRole, pushTask.getResource());
                    if (!continueOnError()) {
                        throw new JobExecutionException("While pushing roles on connector", e);
                    }
                }
            }
        }

        actions.afterAll(handler, results);

        final String result = createReport(results, pushTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result;
    }

    protected boolean continueOnError() {
        return true;
    }
}
