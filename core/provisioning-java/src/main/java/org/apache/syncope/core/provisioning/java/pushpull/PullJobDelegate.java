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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.PullPolicySpec;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.apache.syncope.core.provisioning.api.pushpull.ReconciliationFilterBuilder;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncToken;

public class PullJobDelegate extends AbstractProvisioningJobDelegate<PullTask> implements SyncopePullExecutor {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private PullUtils pullUtils;

    private final Map<ObjectClass, SyncToken> latestSyncTokens = new HashMap<>();

    @Override
    public void setLatestSyncToken(final ObjectClass objectClass, final SyncToken latestSyncToken) {
        latestSyncTokens.put(objectClass, latestSyncToken);
    }

    private void setGroupOwners(final GroupPullResultHandler ghandler) {
        for (Map.Entry<String, String> entry : ghandler.getGroupOwnerMap().entrySet()) {
            Group group = groupDAO.find(entry.getKey());
            if (group == null) {
                throw new NotFoundException("Group " + entry.getKey());
            }

            if (StringUtils.isBlank(entry.getValue())) {
                group.setGroupOwner(null);
                group.setUserOwner(null);
            } else {
                String userKey = pullUtils.findMatchingAnyKey(
                        anyTypeDAO.findUser(),
                        entry.getValue(),
                        ghandler.getProfile().getTask().getResource(),
                        ghandler.getProfile().getConnector());

                if (userKey == null) {
                    String groupKey = pullUtils.findMatchingAnyKey(
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
            final PullTask pullTask,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException {

        LOG.debug("Executing pull on {}", pullTask.getResource());

        List<PullActions> actions = new ArrayList<>();
        for (String className : pullTask.getActionsClassNames()) {
            try {
                Class<?> actionsClass = Class.forName(className);
                PullActions pullActions = (PullActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

                actions.add(pullActions);
            } catch (Exception e) {
                LOG.warn("Class '{}' not found", className, e);
            }
        }

        ProvisioningProfile<PullTask, PullActions> profile = new ProvisioningProfile<>(connector, pullTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(getPullPolicySpec(pullTask).getConflictResolutionAction());

        latestSyncTokens.clear();

        if (!profile.isDryRun()) {
            for (PullActions action : actions) {
                action.beforeAll(profile);
            }
        }

        // First OrgUnits...
        if (pullTask.getResource().getOrgUnit() != null) {
            OrgUnit orgUnit = pullTask.getResource().getOrgUnit();
            OperationOptions options = MappingUtils.buildOperationOptions(orgUnit);

            SyncopePullResultHandler rhandler =
                    (SyncopePullResultHandler) ApplicationContextProvider.getBeanFactory().
                    createBean(RealmPullResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
            rhandler.setProfile(profile);
            rhandler.setPullExecutor(this);

            try {
                switch (pullTask.getPullMode()) {
                    case INCREMENTAL:
                        connector.sync(
                                orgUnit.getObjectClass(),
                                orgUnit.getSyncToken(),
                                rhandler,
                                options);
                        if (!dryRun) {
                            orgUnit.setSyncToken(latestSyncTokens.get(orgUnit.getObjectClass()));
                            resourceDAO.save(orgUnit.getResource());
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        ReconciliationFilterBuilder filterBuilder =
                                (ReconciliationFilterBuilder) ApplicationContextProvider.getBeanFactory().
                                createBean(Class.forName(pullTask.getReconciliationFilterBuilderClassName()),
                                        AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
                        connector.filteredReconciliation(orgUnit.getObjectClass(),
                                filterBuilder,
                                rhandler,
                                options);
                        break;

                    case FULL_RECONCILIATION:
                    default:
                        connector.fullReconciliation(orgUnit.getObjectClass(),
                                rhandler,
                                options);
                        break;
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
            }
        }

        // ...then provisions for any types
        AnyObjectPullResultHandler ahandler = (AnyObjectPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(AnyObjectPullResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ahandler.setProfile(profile);
        ahandler.setPullExecutor(this);

        UserPullResultHandler uhandler = (UserPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(UserPullResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        uhandler.setProfile(profile);
        uhandler.setPullExecutor(this);

        GroupPullResultHandler ghandler = (GroupPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(GroupPullResultHandlerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        ghandler.setProfile(profile);
        ghandler.setPullExecutor(this);

        for (Provision provision : pullTask.getResource().getProvisions()) {
            if (provision.getMapping() != null) {
                SyncopePullResultHandler handler;
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
                    OperationOptions options = MappingUtils.buildOperationOptions(mapItems);

                    switch (pullTask.getPullMode()) {
                        case INCREMENTAL:
                            connector.sync(
                                    provision.getObjectClass(),
                                    provision.getSyncToken(),
                                    handler,
                                    options);
                            if (!dryRun) {
                                provision.setSyncToken(latestSyncTokens.get(provision.getObjectClass()));
                                resourceDAO.save(provision.getResource());
                            }
                            break;

                        case FILTERED_RECONCILIATION:
                            ReconciliationFilterBuilder filterBuilder =
                                    (ReconciliationFilterBuilder) ApplicationContextProvider.getBeanFactory().
                                    createBean(Class.forName(pullTask.getReconciliationFilterBuilderClassName()),
                                            AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
                            connector.filteredReconciliation(provision.getObjectClass(),
                                    filterBuilder,
                                    handler,
                                    options);
                            break;

                        case FULL_RECONCILIATION:
                        default:
                            connector.fullReconciliation(provision.getObjectClass(),
                                    handler,
                                    options);
                            break;
                    }
                } catch (Throwable t) {
                    throw new JobExecutionException("While pulling from connector", t);
                }
            }
        }
        try {
            setGroupOwners(ghandler);
        } catch (Exception e) {
            LOG.error("While setting group owners", e);
        }

        if (!profile.isDryRun()) {
            for (PullActions action : actions) {
                action.afterAll(profile);
            }
        }

        String result = createReport(profile.getResults(), pullTask.getResource(), dryRun);
        LOG.debug("Pull result: {}", result);
        return result;
    }

    private PullPolicySpec getPullPolicySpec(final ProvisioningTask task) {
        PullPolicySpec pullPolicySpec;

        if (task instanceof PullTask) {
            pullPolicySpec = task.getResource().getPullPolicy() == null
                    ? null
                    : task.getResource().getPullPolicy().getSpecification();
        } else {
            pullPolicySpec = null;
        }

        // step required because the call <policy>.getSpecification() could return a null value
        return pullPolicySpec == null ? new PullPolicySpec() : pullPolicySpec;
    }
}
