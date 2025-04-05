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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.StoppableSchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;

public class PullJobDelegate
        extends AbstractPullExecutor<PullTask>
        implements SyncopePullExecutor, StoppableSchedTaskJobDelegate {

    protected Optional<ReconFilterBuilder> perContextReconFilterBuilder = Optional.empty();

    protected ReconFilterBuilder getReconFilterBuilder(final PullTask task) throws ClassNotFoundException {
        return ImplementationManager.build(
                task.getReconFilterBuilder(),
                () -> perContextReconFilterBuilder.orElse(null),
                instance -> perContextReconFilterBuilder = Optional.of(instance));
    }

    @Override
    protected void init(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context) throws JobExecutionException {

        super.init(taskType, taskKey, context);

        profile = new ProvisioningProfile<>(
                connector,
                taskType,
                task,
                Optional.ofNullable(task.getResource().getInboundPolicy()).
                        map(InboundPolicy::getConflictResolutionAction).
                        orElse(ConflictResolutionAction.IGNORE),
                getInboundActions(task.getActions()),
                executor,
                context.isDryRun());

        dispatcher = new PullResultHandlerDispatcher(profile, this);
    }

    @Override
    public void stop() {
        Optional.ofNullable(dispatcher).ifPresent(PullResultHandlerDispatcher::stop);
    }

    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Executing pull on {}", task.getResource());

        if (!profile.isDryRun()) {
            for (InboundActions action : profile.getActions()) {
                action.beforeAll(profile);
            }
        }

        // First realms...
        if (task.getResource().getOrgUnit() != null) {
            setStatus("Pulling " + task.getResource().getOrgUnit().getObjectClass());

            OrgUnit orgUnit = task.getResource().getOrgUnit();

            Set<String> moreAttrsToGet = new HashSet<>();
            profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, orgUnit)));
            OperationOptions options = MappingUtils.buildOperationOptions(
                    MappingUtils.getInboundItems(orgUnit.getItems().stream()), moreAttrsToGet.toArray(String[]::new));

            dispatcher.addHandlerSupplier(orgUnit.getObjectClass(), () -> {
                RealmPullResultHandler handler = buildRealmHandler();
                handler.setProfile(profile);
                return handler;
            });

            try {
                switch (task.getPullMode()) {
                    case INCREMENTAL:
                        if (!context.isDryRun()) {
                            setLatestSyncToken(
                                    orgUnit.getObjectClass(),
                                    ConnObjectUtils.toSyncToken(orgUnit.getSyncToken()));
                        }

                        connector.sync(new ObjectClass(orgUnit.getObjectClass()),
                                ConnObjectUtils.toSyncToken(orgUnit.getSyncToken()),
                                dispatcher,
                                options);

                        if (!context.isDryRun()) {
                            orgUnit.setSyncToken(
                                    ConnObjectUtils.toString(latestSyncTokens.get(orgUnit.getObjectClass())));
                            resourceDAO.save(task.getResource());
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        connector.filteredReconciliation(new ObjectClass(orgUnit.getObjectClass()),
                                getReconFilterBuilder(task),
                                dispatcher,
                                options);
                        break;

                    case FULL_RECONCILIATION:
                    default:
                        connector.fullReconciliation(
                                new ObjectClass(orgUnit.getObjectClass()),
                                dispatcher,
                                options);
                        break;
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
            }
        }

        // ...then provisions for any types
        ghandler = buildGroupHandler();
        for (Provision provision : task.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).sorted(provisionSorter).
                toList()) {

            setStatus("Pulling " + provision.getObjectClass());

            AnyType anyType = anyTypeDAO.findById(provision.getAnyType()).
                    orElseThrow(() -> new NotFoundException("AnyType" + provision.getAnyType()));

            dispatcher.addHandlerSupplier(provision.getObjectClass(), () -> {
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
                return handler;
            });

            boolean setSyncTokens = false;
            try {
                Set<String> moreAttrsToGet = new HashSet<>();
                profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, provision)));
                Stream<Item> mapItems = Stream.concat(
                        MappingUtils.getInboundItems(provision.getMapping().getItems().stream()),
                        virSchemaDAO.findByResourceAndAnyType(
                                task.getResource().getKey(), anyType.getKey()).stream().
                                map(VirSchema::asLinkingMappingItem));
                OperationOptions options = MappingUtils.buildOperationOptions(
                        mapItems, moreAttrsToGet.toArray(String[]::new));

                switch (task.getPullMode()) {
                    case INCREMENTAL:
                        if (!context.isDryRun()) {
                            setLatestSyncToken(
                                    provision.getObjectClass(),
                                    ConnObjectUtils.toSyncToken(provision.getSyncToken()));
                        }

                        connector.sync(
                                new ObjectClass(provision.getObjectClass()),
                                ConnObjectUtils.toSyncToken(provision.getSyncToken()),
                                dispatcher,
                                options);

                        if (!context.isDryRun()) {
                            setSyncTokens = true;
                        }
                        break;

                    case FILTERED_RECONCILIATION:
                        connector.filteredReconciliation(new ObjectClass(provision.getObjectClass()),
                                getReconFilterBuilder(task),
                                dispatcher,
                                options);
                        break;

                    case FULL_RECONCILIATION:
                    default:
                        connector.fullReconciliation(
                                new ObjectClass(provision.getObjectClass()),
                                dispatcher,
                                options);
                }
            } catch (Throwable t) {
                throw new JobExecutionException("While pulling from connector", t);
            } finally {
                if (setSyncTokens) {
                    latestSyncTokens.forEach((objectClass, syncToken) -> {
                        task.getResource().getProvisionByObjectClass(objectClass).
                                ifPresent(p -> p.setSyncToken(ConnObjectUtils.toString(syncToken)));
                        Optional.ofNullable(task.getResource().getOrgUnit()).
                                filter(ou -> objectClass.equals(ou.getObjectClass())).
                                ifPresent(ou -> ou.setSyncToken(ConnObjectUtils.toString(syncToken)));
                    });
                    resourceDAO.save(task.getResource());
                }
            }
        }

        dispatcher.shutdown();

        for (Provision provision : task.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null && provision.getUidOnCreate() != null).
                sorted(provisionSorter).toList()) {

            try {
                AnyType anyType = anyTypeDAO.findById(provision.getAnyType()).
                        orElseThrow(() -> new NotFoundException("AnyType" + provision.getAnyType()));
                AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());
                profile.getResults().stream().
                        filter(result -> result.getUidValue() != null && result.getKey() != null
                        && result.getOperation() == ResourceOperation.CREATE
                        && result.getAnyType().equals(provision.getAnyType())).
                        forEach(result -> anyUtils.addAttr(
                        validator,
                        result.getKey(),
                        plainSchemaDAO.findById(provision.getUidOnCreate()).
                                orElseThrow(() -> new NotFoundException("PlainSchema " + provision.getUidOnCreate())),
                        result.getUidValue()));
            } catch (Throwable t) {
                LOG.error("While setting UID on create", t);
            }
        }

        try {
            setGroupOwners();
        } catch (Exception e) {
            LOG.error("While setting group owners", e);
        }

        if (!profile.isDryRun()) {
            for (InboundActions action : profile.getActions()) {
                action.afterAll(profile);
            }
        }

        setStatus("Pull done");

        String result = createReport(profile.getResults(), task.getResource(), context.isDryRun());
        LOG.debug("Pull result: {}", result);
        return result;
    }
}
