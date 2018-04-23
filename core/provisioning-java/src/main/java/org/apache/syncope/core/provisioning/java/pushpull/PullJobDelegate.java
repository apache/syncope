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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.collections.IteratorChain;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.spring.ImplementationManager;

public class PullJobDelegate extends AbstractProvisioningJobDelegate<PullTask> implements SyncopePullExecutor {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected PullUtils pullUtils;

    protected final Map<ObjectClass, SyncToken> latestSyncTokens = new HashMap<>();

    protected final Map<ObjectClass, MutablePair<Integer, String>> handled = new HashMap<>();

    protected ProvisioningProfile<PullTask, PullActions> profile;

    @Override
    public void setLatestSyncToken(final ObjectClass objectClass, final SyncToken latestSyncToken) {
        latestSyncTokens.put(objectClass, latestSyncToken);
    }

    @Override
    public void reportHandled(final ObjectClass objectClass, final Name name) {
        MutablePair<Integer, String> pair = handled.get(objectClass);
        if (pair == null) {
            pair = MutablePair.of(0, null);
            handled.put(objectClass, pair);
        }
        pair.setLeft(pair.getLeft() + 1);
        pair.setRight(name.getNameValue());
    }

    @Override
    public boolean wasInterruptRequested() {
        return interrupt;
    }

    @Override
    public void setInterrupted() {
        this.interrupted = true;
    }

    @Override
    public String currentStatus() {
        synchronized (status) {
            if (!handled.isEmpty()) {
                StringBuilder builder = new StringBuilder("Processed:\n");
                handled.forEach((key, value) -> {
                    builder.append(' ').append(value.getLeft()).append('\t').
                            append(key.getObjectClassValue()).
                            append(" / latest: ").append(value.getRight()).
                            append('\n');
                });
                status.set(builder.toString());
            }
        }
        return status.get();
    }

    protected void setGroupOwners(
            final GroupPullResultHandler ghandler,
            final boolean userIgnoreCaseMatch,
            final boolean groupIgnoreCaseMatch) {

        ghandler.getGroupOwnerMap().entrySet().stream().map(entry -> {
            Group group = groupDAO.find(entry.getKey());
            if (group == null) {
                throw new NotFoundException("Group " + entry.getKey());
            }
            if (StringUtils.isBlank(entry.getValue())) {
                group.setGroupOwner(null);
                group.setUserOwner(null);
            } else {
                Optional<String> userKey = pullUtils.match(
                        anyTypeDAO.findUser(),
                        entry.getValue(),
                        ghandler.getProfile().getTask().getResource(),
                        ghandler.getProfile().getConnector(),
                        userIgnoreCaseMatch);
                if (userKey.isPresent()) {
                    group.setUserOwner(userDAO.find(userKey.get()));
                } else {
                    pullUtils.match(
                            anyTypeDAO.findGroup(),
                            entry.getValue(),
                            ghandler.getProfile().getTask().getResource(),
                            ghandler.getProfile().getConnector(),
                            groupIgnoreCaseMatch).
                            ifPresent(groupKey -> group.setGroupOwner(groupDAO.find(groupKey)));
                }
            }
            return group;
        }).forEachOrdered(group -> {
            groupDAO.save(group);
        });
    }

    protected RealmPullResultHandler buildRealmHandler() {
        return (RealmPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultRealmPullResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    protected AnyObjectPullResultHandler buildAnyObjectHandler() {
        return (AnyObjectPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultAnyObjectPullResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    protected UserPullResultHandler buildUserHandler() {
        return (UserPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultUserPullResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    protected GroupPullResultHandler buildGroupHandler() {
        GroupPullResultHandler handler = (GroupPullResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultGroupPullResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
        handler.setProfile(profile);
        handler.setPullExecutor(this);

        return handler;
    }

    @Override
    protected String doExecuteProvisioning(
            final PullTask pullTask,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException {

        LOG.debug("Executing pull on {}", pullTask.getResource());

        List<PullActions> actions = new ArrayList<>();
        pullTask.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        profile = new ProvisioningProfile<>(connector, pullTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setResAct(pullTask.getResource().getPullPolicy() == null
                ? ConflictResolutionAction.IGNORE
                : pullTask.getResource().getPullPolicy().getConflictResolutionAction());

        latestSyncTokens.clear();

        if (!profile.isDryRun()) {
            for (PullActions action : actions) {
                action.beforeAll(profile);
            }
        }

        status.set("Initialization completed");

        // First realms...
        if (pullTask.getResource().getOrgUnit() != null) {
            status.set("Pulling " + pullTask.getResource().getOrgUnit().getObjectClass().getObjectClassValue());

            OrgUnit orgUnit = pullTask.getResource().getOrgUnit();
            OperationOptions options = MappingUtils.buildOperationOptions(
                    MappingUtils.getPullItems(orgUnit.getItems()).iterator());

            RealmPullResultHandler handler = buildRealmHandler();
            handler.setProfile(profile);
            handler.setPullExecutor(this);

            try {
                switch (pullTask.getPullMode()) {
                    case INCREMENTAL:
                        if (!dryRun) {
                            latestSyncTokens.put(orgUnit.getObjectClass(), orgUnit.getSyncToken());
                        }

                        connector.sync(
                                orgUnit.getObjectClass(),
                                orgUnit.getSyncToken(),
                                handler,
                                options);

                        if (!dryRun) {
                            orgUnit.setSyncToken(latestSyncTokens.get(orgUnit.getObjectClass()));
                            resourceDAO.save(orgUnit.getResource());
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        ReconFilterBuilder filterBuilder =
                                ImplementationManager.build(pullTask.getReconFilterBuilder());
                        connector.filteredReconciliation(orgUnit.getObjectClass(),
                                filterBuilder,
                                handler,
                                options);
                        break;

                    case FULL_RECONCILIATION:
                    default:
                        connector.fullReconciliation(orgUnit.getObjectClass(),
                                handler,
                                options);
                        break;
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
            }
        }

        // ...then provisions for any types
        SyncopePullResultHandler handler;
        GroupPullResultHandler ghandler = buildGroupHandler();
        boolean userIgnoreCaseMatch = false;
        boolean groupIgnoreCaseMatch = false;
        for (Provision provision : pullTask.getResource().getProvisions()) {
            if (provision.getMapping() != null) {
                if (provision.getAnyType().getKind() == AnyTypeKind.USER) {
                    userIgnoreCaseMatch = provision.isIgnoreCaseMatch();
                } else if (provision.getAnyType().getKind() == AnyTypeKind.GROUP) {
                    groupIgnoreCaseMatch = provision.isIgnoreCaseMatch();
                }

                status.set("Pulling " + provision.getObjectClass().getObjectClassValue());

                switch (provision.getAnyType().getKind()) {
                    case USER:
                        handler = buildUserHandler();
                        break;

                    case GROUP:
                        handler = ghandler;
                        break;

                    case ANY_OBJECT:
                    default:
                        handler = buildAnyObjectHandler();
                }
                handler.setProfile(profile);
                handler.setPullExecutor(this);

                try {
                    Set<MappingItem> linkingMappingItems = virSchemaDAO.findByProvision(provision).stream().
                            map(schema -> schema.asLinkingMappingItem()).collect(Collectors.toSet());
                    Iterator<MappingItem> mapItems = new IteratorChain<>(
                            provision.getMapping().getItems().iterator(),
                            linkingMappingItems.iterator());
                    OperationOptions options = MappingUtils.buildOperationOptions(mapItems);

                    switch (pullTask.getPullMode()) {
                        case INCREMENTAL:
                            if (!dryRun) {
                                latestSyncTokens.put(provision.getObjectClass(), provision.getSyncToken());
                            }

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
                            ReconFilterBuilder filterBuilder =
                                    ImplementationManager.build(pullTask.getReconFilterBuilder());
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
            setGroupOwners(ghandler, userIgnoreCaseMatch, groupIgnoreCaseMatch);
        } catch (Exception e) {
            LOG.error("While setting group owners", e);
        }

        if (!profile.isDryRun()) {
            for (PullActions action : actions) {
                action.afterAll(profile);
            }
        }

        status.set("Pull done");

        String result = createReport(profile.getResults(), pullTask.getResource(), dryRun);
        LOG.debug("Pull result: {}", result);
        return result;
    }
}
