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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PullMatch;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.ImplementationManager;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public class PullJobDelegate extends AbstractProvisioningJobDelegate<PullTask> implements SyncopePullExecutor {

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    protected final Map<String, SyncToken> latestSyncTokens = new HashMap<>();

    protected final Map<String, MutablePair<Integer, String>> handled = new HashMap<>();

    protected ProvisioningProfile<PullTask, PullActions> profile;

    @Override
    public void setLatestSyncToken(final String objectClass, final SyncToken latestSyncToken) {
        latestSyncTokens.put(objectClass, latestSyncToken);
    }

    @Override
    public void reportHandled(final String objectClass, final Name name) {
        MutablePair<Integer, String> pair = Optional.ofNullable(handled.get(objectClass)).orElseGet(() -> {
            MutablePair<Integer, String> p = MutablePair.of(0, null);
            handled.put(objectClass, p);
            return p;
        });
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
                handled.forEach((key, value) -> builder.append(' ').append(value.getLeft()).append('\t').
                        append(key).
                        append(" / latest: ").append(value.getRight()).
                        append('\n'));
                status.set(builder.toString());
            }
        }
        return status.get();
    }

    protected void setGroupOwners(final GroupPullResultHandler ghandler) {
        ghandler.getGroupOwnerMap().forEach((groupKey, ownerKey) -> {
            Group group = groupDAO.find(groupKey);
            if (group == null) {
                throw new NotFoundException("Group " + groupKey);
            }
            if (StringUtils.isBlank(ownerKey)) {
                group.setGroupOwner(null);
                group.setUserOwner(null);
            } else {
                Optional<PullMatch> match = inboundMatcher.match(
                        anyTypeDAO.findUser(),
                        ownerKey,
                        ghandler.getProfile().getTask().getResource(),
                        ghandler.getProfile().getConnector());
                if (match.isPresent()) {
                    group.setUserOwner((User) match.get().getAny());
                } else {
                    inboundMatcher.match(
                            anyTypeDAO.findGroup(),
                            ownerKey,
                            ghandler.getProfile().getTask().getResource(),
                            ghandler.getProfile().getConnector()).
                            ifPresent(groupMatch -> group.setGroupOwner((Group) groupMatch.getAny()));
                }
            }

            groupDAO.save(group);
        });
    }

    protected List<PullActions> buildPullActions(final PullTask pullTask) {
        List<PullActions> actions = new ArrayList<>();
        pullTask.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });
        return actions;
    }

    protected ReconFilterBuilder buildReconFilterBuilder(final PullTask pullTask)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        return ImplementationManager.build(pullTask.getReconFilterBuilder());
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
            final boolean dryRun,
            final String executor,
            final JobExecutionContext context) throws JobExecutionException {

        LOG.debug("Executing pull on {}", pullTask.getResource());

        List<PullActions> actions = buildPullActions(pullTask);

        profile = new ProvisioningProfile<>(connector, pullTask);
        profile.getActions().addAll(actions);
        profile.setDryRun(dryRun);
        profile.setConflictResolutionAction(pullTask.getResource().getPullPolicy() == null
                ? ConflictResolutionAction.IGNORE
                : pullTask.getResource().getPullPolicy().getConflictResolutionAction());
        profile.setExecutor(executor);

        latestSyncTokens.clear();

        if (!profile.isDryRun()) {
            for (PullActions action : actions) {
                action.beforeAll(profile);
            }
        }

        status.set("Initialization completed");

        // First realms...
        if (pullTask.getResource().getOrgUnit() != null) {
            status.set("Pulling " + pullTask.getResource().getOrgUnit().getObjectClass());

            OrgUnit orgUnit = pullTask.getResource().getOrgUnit();

            Set<String> moreAttrsToGet = new HashSet<>();
            actions.forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(profile, orgUnit)));
            OperationOptions options = MappingUtils.buildOperationOptions(
                    MappingUtils.getPullItems(orgUnit.getItems().stream()), moreAttrsToGet.toArray(String[]::new));

            RealmPullResultHandler handler = buildRealmHandler();
            handler.setProfile(profile);
            handler.setPullExecutor(this);

            try {
                switch (pullTask.getPullMode()) {
                    case INCREMENTAL:
                        if (!dryRun) {
                            latestSyncTokens.put(
                                    orgUnit.getObjectClass(),
                                    ConnObjectUtils.toSyncToken(orgUnit.getSyncToken()));
                        }

                        connector.sync(new ObjectClass(orgUnit.getObjectClass()),
                                ConnObjectUtils.toSyncToken(orgUnit.getSyncToken()),
                                handler,
                                options);

                        if (!dryRun) {
                            orgUnit.setSyncToken(
                                    ConnObjectUtils.toString(latestSyncTokens.get(orgUnit.getObjectClass())));
                            resourceDAO.save(pullTask.getResource());
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        connector.filteredReconciliation(
                                new ObjectClass(orgUnit.getObjectClass()),
                                buildReconFilterBuilder(pullTask),
                                handler,
                                options);
                        break;

                    case FULL_RECONCILIATION:
                    default:
                        connector.fullReconciliation(
                                new ObjectClass(orgUnit.getObjectClass()),
                                handler,
                                options);
                        break;
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
            }
        }

        // ...then provisions for any types
        ProvisionSorter provisionSorter = new DefaultProvisionSorter();
        if (pullTask.getResource().getProvisionSorter() != null) {
            try {
                provisionSorter = ImplementationManager.build(pullTask.getResource().getProvisionSorter());
            } catch (Exception e) {
                LOG.error("While building {}", pullTask.getResource().getProvisionSorter(), e);
            }
        }

        GroupPullResultHandler ghandler = buildGroupHandler();
        for (Provision provision : pullTask.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).sorted(provisionSorter).
                collect(Collectors.toList())) {

            status.set("Pulling " + provision.getObjectClass());

            AnyType anyType = anyTypeDAO.find(provision.getAnyType());

            SyncopePullResultHandler handler;
            switch (anyType.getKind()) {
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
                Set<String> moreAttrsToGet = new HashSet<>();
                actions.forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(profile, provision)));
                Stream<Item> mapItems = Stream.concat(
                        MappingUtils.getPullItems(provision.getMapping().getItems().stream()),
                        virSchemaDAO.find(pullTask.getResource().getKey(), anyType.getKey()).stream().
                                map(VirSchema::asLinkingMappingItem));
                OperationOptions options = MappingUtils.buildOperationOptions(
                        mapItems, moreAttrsToGet.toArray(String[]::new));

                switch (pullTask.getPullMode()) {
                    case INCREMENTAL:
                        if (!dryRun) {
                            latestSyncTokens.put(
                                    provision.getObjectClass(),
                                    ConnObjectUtils.toSyncToken(provision.getSyncToken()));
                        }

                        connector.sync(
                                new ObjectClass(provision.getObjectClass()),
                                ConnObjectUtils.toSyncToken(provision.getSyncToken()),
                                handler,
                                options);

                        if (!dryRun) {
                            provision.setSyncToken(
                                    ConnObjectUtils.toString(latestSyncTokens.get(provision.getObjectClass())));
                            resourceDAO.save(pullTask.getResource());
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        connector.filteredReconciliation(
                                new ObjectClass(provision.getObjectClass()),
                                buildReconFilterBuilder(pullTask),
                                handler,
                                options);
                        break;

                    case FULL_RECONCILIATION:
                    default:
                        connector.fullReconciliation(
                                new ObjectClass(provision.getObjectClass()),
                                handler,
                                options);
                        break;
                }

                if (provision.getUidOnCreate() != null) {
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());
                    profile.getResults().stream().
                            filter(result -> result.getUidValue() != null && result.getKey() != null
                            && result.getOperation() == ResourceOperation.CREATE
                            && result.getAnyType().equals(provision.getAnyType())).
                            forEach(result -> anyUtils.addAttr(
                            result.getKey(), plainSchemaDAO.find(provision.getUidOnCreate()), result.getUidValue()));
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
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

        status.set("Pull done");

        String result = createReport(profile.getResults(), pullTask.getResource(), dryRun);
        LOG.debug("Pull result: {}", result);
        return result;
    }
}
