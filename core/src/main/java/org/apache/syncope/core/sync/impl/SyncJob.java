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
import org.apache.syncope.core.sync.SyncUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.ReferenceMod;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.AbstractSyncTask;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
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

    @Autowired
    protected SyncUtilities syncUtilities;

    protected void setRoleOwners(final RoleSyncResultHandler rhandler)
            throws UnauthorizedRoleException, NotFoundException {

        for (Map.Entry<Long, String> entry : rhandler.getRoleOwnerMap().entrySet()) {
            RoleMod roleMod = new RoleMod();
            roleMod.setId(entry.getKey());

            if (StringUtils.isBlank(entry.getValue())) {
                roleMod.setRoleOwner(null);
                roleMod.setUserOwner(null);
            } else {
                Long userId = syncUtilities.findMatchingAttributableId(
                        ObjectClass.ACCOUNT,
                        entry.getValue(),
                        rhandler.getProfile().getSyncTask().getResource(),
                        rhandler.getProfile().getConnector());

                if (userId == null) {
                    Long roleId = syncUtilities.findMatchingAttributableId(
                            ObjectClass.GROUP,
                            entry.getValue(),
                            rhandler.getProfile().getSyncTask().getResource(),
                            rhandler.getProfile().getConnector());

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
            final Connector connector,
            final UMapping uMapping,
            final RMapping rMapping,
            final boolean dryRun) throws JobExecutionException {
        LOG.debug("Execute synchronization with token {}", syncTask.getResource().getUsyncToken());

        final List<SyncResult> results = new ArrayList<SyncResult>();

        final SyncProfile<SyncTask, SyncActions> profile =
                new SyncProfile<SyncTask, SyncActions>(connector, syncTask);
        profile.setActions(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(getSyncPolicySpec(syncTask).getConflictResolutionAction());
        profile.setResults(results);

        // Prepare handler for SyncDelta objects (users)
        final UserSyncResultHandler uhandler =
                (UserSyncResultHandler) ((DefaultListableBeanFactory) ApplicationContextProvider.
                getApplicationContext().getBeanFactory()).createBean(
                UserSyncResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        // Prepare handler for SyncDelta objects (roles/groups)
        final RoleSyncResultHandler rhandler =
                (RoleSyncResultHandler) ((DefaultListableBeanFactory) ApplicationContextProvider.
                getApplicationContext().getBeanFactory()).createBean(
                RoleSyncResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        rhandler.setProfile(profile);

        if (!profile.isDryRun()) {
            for (SyncActions action : actions) {
                action.beforeAll(profile);
            }
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
                    connector.getAllObjects(ObjectClass.ACCOUNT, uhandler,
                            connector.getOperationOptions(uMapping.getItems()));
                }
                if (rMapping != null) {
                    connector.getAllObjects(ObjectClass.GROUP, rhandler,
                            connector.getOperationOptions(rMapping.getItems()));
                }
            } else {
                if (uMapping != null) {
                    connector.sync(ObjectClass.ACCOUNT, syncTask.getResource().getUsyncToken(), uhandler,
                            connector.getOperationOptions(uMapping.getItems()));
                }
                if (rMapping != null) {
                    connector.sync(ObjectClass.GROUP, syncTask.getResource().getRsyncToken(), rhandler,
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
            setRoleOwners(rhandler);
        } catch (Exception e) {
            LOG.error("While setting role owners", e);
        }

        if (!profile.isDryRun()) {
            for (SyncActions action : actions) {
                action.afterAll(profile, results);
            }
        }

        final String result = createReport(results, syncTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result;
    }

    private SyncPolicySpec getSyncPolicySpec(final AbstractSyncTask syncTask) {
        SyncPolicySpec syncPolicySpec;

        if (syncTask instanceof SyncTask) {
            final SyncPolicy syncPolicy = syncTask.getResource().getSyncPolicy() == null
                    ? policyDAO.getGlobalSyncPolicy()
                    : syncTask.getResource().getSyncPolicy();

            syncPolicySpec = syncPolicy == null ? null : syncPolicy.getSpecification(SyncPolicySpec.class);
        } else {
            syncPolicySpec = null;
        }

        // step required because the call <policy>.getSpecification() could return a null value
        return syncPolicySpec == null ? new SyncPolicySpec() : syncPolicySpec;
    }
}
