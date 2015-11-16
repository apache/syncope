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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.SyncPolicySpec;
import org.apache.syncope.core.misc.utils.MappingUtils;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.AnyObjectSyncResultHandler;
import org.apache.syncope.core.provisioning.api.sync.GroupSyncResultHandler;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.provisioning.api.sync.UserSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.apache.syncope.core.provisioning.api.sync.ReconciliationFilterBuilder;

public class SyncJobDelegate extends AbstractProvisioningJobDelegate<SyncTask> {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    protected SyncUtils syncUtils;

    protected void setGroupOwners(final GroupSyncResultHandler ghandler) {
        for (Map.Entry<Long, String> entry : ghandler.getGroupOwnerMap().entrySet()) {
            Group group = groupDAO.find(entry.getKey());
            if (group == null) {
                throw new NotFoundException("Group " + entry.getKey());
            }

            if (StringUtils.isBlank(entry.getValue())) {
                group.setGroupOwner(null);
                group.setUserOwner(null);
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
                        group.setGroupOwner(groupDAO.find(groupKey));
                    }
                } else {
                    group.setUserOwner(userDAO.find(userKey));
                }
            }

            groupDAO.save(group);
        }
    }

    @Override
    protected String doExecuteProvisioning(
            final SyncTask syncTask,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException {

        LOG.debug("Executing sync on {}", syncTask.getResource());

        List<SyncActions> actions = new ArrayList<>();
        for (String className : syncTask.getActionsClassNames()) {
            try {
                Class<?> actionsClass = Class.forName(className);
                SyncActions syncActions = (SyncActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

                actions.add(syncActions);
            } catch (Exception e) {
                LOG.warn("Class '{}' not found", className, e);
            }
        }

        ProvisioningProfile<SyncTask, SyncActions> profile = new ProvisioningProfile<>(connector, syncTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(getSyncPolicySpec(syncTask).getConflictResolutionAction());

        // Prepare handler for SyncDelta objects (any objects)
        AnyObjectSyncResultHandler ahandler = (AnyObjectSyncResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(AnyObjectSyncResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ahandler.setProfile(profile);

        // Prepare handler for SyncDelta objects (users)
        UserSyncResultHandler uhandler = (UserSyncResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(UserSyncResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);

        // Prepare handler for SyncDelta objects (groups)
        GroupSyncResultHandler ghandler = (GroupSyncResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(GroupSyncResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ghandler.setProfile(profile);

        if (!profile.isDryRun()) {
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
                    Set<MappingItem> linkinMappingItems = new HashSet<>();
                    for (VirSchema virSchema : virSchemaDAO.findByProvision(provision)) {
                        linkinMappingItems.add(virSchema.asLinkingMappingItem());
                    }
                    Iterator<MappingItem> mapItems = IteratorUtils.chainedIterator(
                            provision.getMapping().getItems().iterator(),
                            linkinMappingItems.iterator());

                    switch (syncTask.getSyncMode()) {
                        case INCREMENTAL:
                            SyncToken latestSyncToken = connector.getLatestSyncToken(provision.getObjectClass());
                            connector.sync(
                                    provision.getObjectClass(),
                                    provision.getSyncToken(),
                                    handler,
                                    MappingUtils.buildOperationOptions(mapItems));
                            if (!dryRun) {
                                provision.setSyncToken(latestSyncToken);
                                resourceDAO.save(provision.getResource());
                            }
                            break;

                        case FILTERED_RECONCILIATION:
                            ReconciliationFilterBuilder filterBuilder =
                                    (ReconciliationFilterBuilder) ApplicationContextProvider.getBeanFactory().
                                    createBean(Class.forName(syncTask.getReconciliationFilterBuilderClassName()),
                                            AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
                            connector.filteredReconciliation(
                                    provision.getObjectClass(),
                                    filterBuilder,
                                    handler,
                                    MappingUtils.buildOperationOptions(mapItems));
                            break;

                        case FULL_RECONCILIATION:
                        default:
                            connector.fullReconciliation(
                                    provision.getObjectClass(),
                                    handler,
                                    MappingUtils.buildOperationOptions(mapItems));
                            break;
                    }
                } catch (Throwable t) {
                    throw new JobExecutionException("While syncing from connector", t);
                }
            }
        }

        try {
            setGroupOwners(ghandler);
        } catch (Exception e) {
            LOG.error("While setting group owners", e);
        }

        if (!profile.isDryRun()) {
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
                    : task.getResource().getSyncPolicy().getSpecification();
        } else {
            syncPolicySpec = null;
        }

        // step required because the call <policy>.getSpecification() could return a null value
        return syncPolicySpec == null ? new SyncPolicySpec() : syncPolicySpec;
    }
}
