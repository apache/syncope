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

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.ReferenceMod;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.job.SyncJob;
import org.apache.syncope.core.provisioning.api.sync.AnyObjectSyncResultHandler;
import org.apache.syncope.core.provisioning.api.sync.GroupSyncResultHandler;
import org.apache.syncope.core.provisioning.api.sync.UserSyncResultHandler;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
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
     * Group workflow adapter.
     */
    @Autowired
    private GroupWorkflowAdapter gwfAdapter;

    @Autowired
    protected SyncUtils syncUtils;

    protected void setGroupOwners(final GroupSyncResultHandler ghandler) {
        for (Map.Entry<Long, String> entry : ghandler.getGroupOwnerMap().entrySet()) {
            GroupMod groupMod = new GroupMod();
            groupMod.setKey(entry.getKey());

            if (StringUtils.isBlank(entry.getValue())) {
                groupMod.setGroupOwner(null);
                groupMod.setUserOwner(null);
            } else {
                Long userKey = syncUtils.findMatchingAnyKey(
                        anyTypeDAO.findUser(),
                        entry.getValue(),
                        ghandler.getProfile().getTask().getResource(),
                        ghandler.getProfile().getConnector());

                if (userKey == null) {
                    Long groupKey = syncUtils.findMatchingAnyKey(
                            anyTypeDAO.findGroup(),
                            entry.getValue(),
                            ghandler.getProfile().getTask().getResource(),
                            ghandler.getProfile().getConnector());

                    if (groupKey != null) {
                        groupMod.setGroupOwner(new ReferenceMod(groupKey));
                    }
                } else {
                    groupMod.setUserOwner(new ReferenceMod(userKey));
                }
            }

            gwfAdapter.update(groupMod);
        }
    }

    @Override
    protected String executeWithSecurityContext(
            final SyncTask syncTask,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException {

        LOG.debug("Executing sync on {}", syncTask.getResource());

        ProvisioningProfile<SyncTask, SyncActions> profile = new ProvisioningProfile<>(connector, syncTask);
        if (actions != null) {
            profile.getActions().addAll(actions);
        }
        profile.setDryRun(dryRun);
        profile.setResAct(getSyncPolicySpec(syncTask).getConflictResolutionAction());

        // Prepare handler for SyncDelta objects (any objects)
        AnyObjectSyncResultHandler ahandler =
                (AnyObjectSyncResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(AnyObjectSyncResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ahandler.setProfile(profile);

        // Prepare handler for SyncDelta objects (users)
        UserSyncResultHandler uhandler =
                (UserSyncResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(UserSyncResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        // Prepare handler for SyncDelta objects (groups)
        GroupSyncResultHandler ghandler =
                (GroupSyncResultHandler) ApplicationContextProvider.getApplicationContext().getBeanFactory().
                createBean(GroupSyncResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ghandler.setProfile(profile);

        if (actions != null && !profile.isDryRun()) {
            for (SyncActions action : actions) {
                action.beforeAll(profile);
            }
        }

        for (Provision provision : syncTask.getResource().getProvisions()) {
            if (provision.getMapping() != null) {
                SyncResultsHandler handler;
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

                try {
                    SyncToken latestSyncToken = null;
                    if (!syncTask.isFullReconciliation()) {
                        latestSyncToken = connector.getLatestSyncToken(provision.getObjectClass());
                    }

                    if (syncTask.isFullReconciliation()) {
                        connector.getAllObjects(provision.getObjectClass(), handler,
                                connector.getOperationOptions(provision.getMapping().getItems()));
                    } else {
                        connector.sync(provision.getObjectClass(), provision.getSyncToken(), handler,
                                connector.getOperationOptions(provision.getMapping().getItems()));
                    }

                    if (!dryRun && !syncTask.isFullReconciliation()) {
                        try {
                            provision.setSyncToken(latestSyncToken);
                            resourceDAO.save(provision.getResource());
                        } catch (Exception e) {
                            throw new JobExecutionException("While updating SyncToken", e);
                        }
                    }
                } catch (Throwable t) {
                    throw new JobExecutionException("While syncing on connector", t);
                }
            }
        }

        try {
            setGroupOwners(ghandler);
        } catch (Exception e) {
            LOG.error("While setting group owners", e);
        }

        if (actions != null && !profile.isDryRun()) {
            for (SyncActions action : actions) {
                action.afterAll(profile);
            }
        }

        String result = createReport(profile.getResults(), syncTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result;
    }

    private SyncPolicySpec getSyncPolicySpec(final ProvisioningTask task) {
        SyncPolicySpec syncPolicySpec;

        if (task instanceof SyncTask) {
            syncPolicySpec = task.getResource().getSyncPolicy() == null
                    ? null
                    : task.getResource().getSyncPolicy().getSpecification(SyncPolicySpec.class);
        } else {
            syncPolicySpec = null;
        }

        // step required because the call <policy>.getSpecification() could return a null value
        return syncPolicySpec == null ? new SyncPolicySpec() : syncPolicySpec;
    }
}
