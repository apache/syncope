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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PullMatch;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
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
import org.apache.syncope.core.spring.implementation.ImplementationManager;
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

    @Autowired
    protected PlainAttrValidationManager validator;

    protected final Map<String, SyncToken> latestSyncTokens = new HashMap<>();

    protected ProvisioningProfile<PullTask, PullActions> profile;

    protected final Map<String, MutablePair<Integer, String>> handled = new HashMap<>();

    protected final Map<String, PullActions> perContextActions = new ConcurrentHashMap<>();

    protected Optional<ReconFilterBuilder> perContextReconFilterBuilder = Optional.empty();

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

        if (!handled.isEmpty()) {
            StringBuilder builder = new StringBuilder("Processed:\n");
            handled.forEach((k, v) -> builder.append(' ').append(v.getLeft()).append('\t').
                    append(k).
                    append(" / latest: ").append(v.getRight()).
                    append('\n'));
            setStatus(builder.toString());
        }
    }

    @Override
    public boolean wasInterruptRequested() {
        return interrupt;
    }

    @Override
    public void setInterrupted() {
        this.interrupted = true;
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

    protected List<PullActions> getPullActions(final List<? extends Implementation> impls) {
        List<PullActions> result = new ArrayList<>();

        impls.forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return result;
    }

    protected ReconFilterBuilder getReconFilterBuilder(final PullTask pullTask) throws ClassNotFoundException {
        return ImplementationManager.build(
                pullTask.getReconFilterBuilder(),
                () -> perContextReconFilterBuilder.orElse(null),
                instance -> perContextReconFilterBuilder = Optional.of(instance));
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

        profile = new ProvisioningProfile<>(connector, pullTask);
        profile.getActions().addAll(getPullActions(pullTask.getActions()));
        profile.setDryRun(dryRun);
        profile.setConflictResolutionAction(pullTask.getResource().getPullPolicy() == null
                ? ConflictResolutionAction.IGNORE
                : pullTask.getResource().getPullPolicy().getConflictResolutionAction());
        profile.setExecutor(executor);

        latestSyncTokens.clear();

        if (!profile.isDryRun()) {
            for (PullActions action : profile.getActions()) {
                action.beforeAll(profile);
            }
        }

        setStatus("Initialization completed");

        // First realms...
        if (pullTask.getResource().getOrgUnit() != null) {
            setStatus("Pulling " + pullTask.getResource().getOrgUnit().getObjectClass());

            OrgUnit orgUnit = pullTask.getResource().getOrgUnit();

            Set<String> moreAttrsToGet = new HashSet<>();
            profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, orgUnit)));
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
                        connector.filteredReconciliation(new ObjectClass(orgUnit.getObjectClass()),
                                getReconFilterBuilder(pullTask),
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
        ProvisionSorter provisionSorter = getProvisionSorter(pullTask);

        GroupPullResultHandler ghandler = buildGroupHandler();
        for (Provision provision : pullTask.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).sorted(provisionSorter).
                collect(Collectors.toList())) {

            setStatus("Pulling " + provision.getObjectClass());

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

            boolean setSyncTokens = false;
            try {
                Set<String> moreAttrsToGet = new HashSet<>();
                profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, provision)));
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
                            setSyncTokens = true;
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        connector.filteredReconciliation(new ObjectClass(provision.getObjectClass()),
                                getReconFilterBuilder(pullTask),
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
                            validator,
                            result.getKey(),
                            plainSchemaDAO.find(provision.getUidOnCreate()), result.getUidValue()));
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
            } finally {
                if (setSyncTokens) {
                    latestSyncTokens.forEach((objectClass, syncToken) -> pullTask.getResource().
                            getProvisionByObjectClass(objectClass).
                            ifPresent(p -> p.setSyncToken(ConnObjectUtils.toString(syncToken))));
                    resourceDAO.save(pullTask.getResource());
                }
            }
        }
        try {
            setGroupOwners(ghandler);
        } catch (Exception e) {
            LOG.error("While setting group owners", e);
        }

        if (!profile.isDryRun()) {
            for (PullActions action : profile.getActions()) {
                action.afterAll(profile);
            }
        }

        setStatus("Pull done");

        String result = createReport(profile.getResults(), pullTask.getResource(), dryRun);
        LOG.debug("Pull result: {}", result);
        return result;
    }
}
