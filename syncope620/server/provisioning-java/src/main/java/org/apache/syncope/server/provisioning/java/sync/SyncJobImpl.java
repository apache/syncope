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
package org.apache.syncope.server.provisioning.java.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.ReferenceMod;
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.entity.ExternalResource;
import org.apache.syncope.server.persistence.api.entity.SyncPolicy;
import org.apache.syncope.server.persistence.api.entity.role.RMapping;
import org.apache.syncope.server.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.server.persistence.api.entity.task.SyncTask;
import org.apache.syncope.server.persistence.api.entity.user.UMapping;
import org.apache.syncope.server.provisioning.api.Connector;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.server.provisioning.api.sync.SyncActions;
import org.apache.syncope.server.misc.security.UnauthorizedRoleException;
import org.apache.syncope.server.misc.spring.ApplicationContextProvider;
import org.apache.syncope.server.provisioning.api.job.SyncJob;
import org.apache.syncope.server.workflow.api.RoleWorkflowAdapter;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Job for executing synchronization (from external resource) tasks.
 *
 * @see AbstractProvisioningJob
 * @see SyncTask
 */
public class SyncJobImpl extends AbstractProvisioningJob<SyncTask, SyncActions> implements SyncJob {

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
            roleMod.setKey(entry.getKey());

            if (StringUtils.isBlank(entry.getValue())) {
                roleMod.setRoleOwner(null);
                roleMod.setUserOwner(null);
            } else {
                Long userId = syncUtilities.findMatchingAttributableId(
                        ObjectClass.ACCOUNT,
                        entry.getValue(),
                        rhandler.getProfile().getTask().getResource(),
                        rhandler.getProfile().getConnector());

                if (userId == null) {
                    Long roleId = syncUtilities.findMatchingAttributableId(
                            ObjectClass.GROUP,
                            entry.getValue(),
                            rhandler.getProfile().getTask().getResource(),
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

        final List<ProvisioningResult> results = new ArrayList<>();

        final ProvisioningProfile<SyncTask, SyncActions> profile = new ProvisioningProfile<>(connector, syncTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(getSyncPolicySpec(syncTask).getConflictResolutionAction());
        profile.getResults().addAll(results);

        // Prepare handler for SyncDelta objects (users)
        final UserSyncResultHandler uhandler =
                (UserSyncResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().createBean(
                        UserSyncResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        // Prepare handler for SyncDelta objects (roles/groups)
        final RoleSyncResultHandler rhandler =
                (RoleSyncResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().createBean(
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
                    ExternalResource resource = resourceDAO.find(syncTask.getResource().getKey());
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

    private SyncPolicySpec getSyncPolicySpec(final ProvisioningTask task) {
        SyncPolicySpec syncPolicySpec;

        if (task instanceof SyncTask) {
            final SyncPolicy syncPolicy = task.getResource().getSyncPolicy() == null
                    ? policyDAO.getGlobalSyncPolicy()
                    : task.getResource().getSyncPolicy();

            syncPolicySpec = syncPolicy == null ? null : syncPolicy.getSpecification(SyncPolicySpec.class);
        } else {
            syncPolicySpec = null;
        }

        // step required because the call <policy>.getSpecification() could return a null value
        return syncPolicySpec == null ? new SyncPolicySpec() : syncPolicySpec;
    }
}
