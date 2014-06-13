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
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.ReferenceMod;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.role.RMapping;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.sync.SyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Job for executing synchronization (from external resource) tasks.
 *
 * @see AbstractSyncJob
 * @see SyncTask
 */
public class SyncJob extends AbstractSyncJob<SyncTask, SyncActions> {

    /**
     * Role workflow adapter.
     */
    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    protected void setRoleOwners(final SyncopeSyncResultHandler handler)
            throws UnauthorizedRoleException, NotFoundException {

        for (Map.Entry<Long, String> entry : handler.getRoleOwnerMap().entrySet()) {
            RoleMod roleMod = new RoleMod();
            roleMod.setId(entry.getKey());

            if (StringUtils.isBlank(entry.getValue())) {
                roleMod.setRoleOwner(null);
                roleMod.setUserOwner(null);
            } else {
                Long userId = handler.findMatchingAttributableId(ObjectClass.ACCOUNT, entry.getValue());
                if (userId == null) {
                    Long roleId = handler.findMatchingAttributableId(ObjectClass.GROUP, entry.getValue());
                    if (roleId != null) {
                        roleMod.setRoleOwner(new ReferenceMod(roleId));
                    }
                } else {
                    roleMod.setUserOwner(new ReferenceMod(userId));
                }
            }

            rwfAdapter.update(roleMod);
        }
    }

    @Override
    protected String executeWithSecurityContext(
            final SyncTask syncTask,
            final SyncPolicySpec syncPolicySpec,
            final Connector connector,
            final UMapping uMapping,
            final RMapping rMapping,
            final boolean dryRun) throws JobExecutionException {
        LOG.debug("Execute synchronization with token {}", syncTask.getResource().getUsyncToken());

        final List<SyncResult> results = new ArrayList<SyncResult>();

        // Prepare handler for SyncDelta objects
        final SyncopeSyncResultHandler handler =
                (SyncopeSyncResultHandler) ((DefaultListableBeanFactory) ApplicationContextProvider.
                getApplicationContext().getBeanFactory()).createBean(
                        SyncopeSyncResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        handler.setConnector(connector);
        handler.setActions(actions);
        handler.setDryRun(dryRun);
        handler.setResAct(syncPolicySpec.getConflictResolutionAction());
        handler.setResults(results);
        handler.setSyncTask(syncTask);

        for (SyncActions action : actions) {
            action.beforeAll(handler);
        }
        try {
            SyncToken latestUSyncToken = null;
            if (uMapping != null && !syncTask.isFullReconciliation()) {
                latestUSyncToken = connector.getLatestSyncToken(ObjectClass.ACCOUNT);
            }
            SyncToken latestRSyncToken = null;
            if (rMapping != null && !syncTask.isFullReconciliation()) {
                latestRSyncToken = connector.getLatestSyncToken(ObjectClass.GROUP);
            }

            if (syncTask.isFullReconciliation()) {
                if (uMapping != null) {
                    connector.getAllObjects(ObjectClass.ACCOUNT, handler,
                            connector.getOperationOptions(uMapping.getItems()));
                }
                if (rMapping != null) {
                    connector.getAllObjects(ObjectClass.GROUP, handler,
                            connector.getOperationOptions(rMapping.getItems()));
                }
            } else {
                if (uMapping != null) {
                    connector.sync(ObjectClass.ACCOUNT, syncTask.getResource().getUsyncToken(), handler,
                            connector.getOperationOptions(uMapping.getItems()));
                }
                if (rMapping != null) {
                    connector.sync(ObjectClass.GROUP, syncTask.getResource().getRsyncToken(), handler,
                            connector.getOperationOptions(rMapping.getItems()));
                }
            }

            if (!dryRun && !syncTask.isFullReconciliation()) {
                try {
                    ExternalResource resource = resourceDAO.find(syncTask.getResource().getName());
                    if (uMapping != null) {
                        resource.setUsyncToken(latestUSyncToken);
                    }
                    if (rMapping != null) {
                        resource.setRsyncToken(latestRSyncToken);
                    }
                    resourceDAO.save(resource);
                } catch (Exception e) {
                    throw new JobExecutionException("While updating SyncToken", e);
                }
            }
        } catch (Exception e) {
            throw new JobExecutionException("While syncing on connector", e);
        }

        try {
            setRoleOwners(handler);
        } catch (Exception e) {
            LOG.error("While setting role owners", e);
        }

        for (SyncActions action : actions) {
            action.afterAll(handler, results);
        }

        final String result = createReport(results, syncTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result;
    }
}
