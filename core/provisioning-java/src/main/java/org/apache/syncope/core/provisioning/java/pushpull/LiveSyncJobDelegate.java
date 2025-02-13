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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.LiveSyncDeltaMapper;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.StoppableSchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class LiveSyncJobDelegate
        extends AbstractProvisioningJobDelegate<LiveSyncTask>
        implements StoppableSchedTaskJobDelegate {

    protected static record LiveSyncInfo(
            Provision provision,
            OrgUnit orgUnit,
            ObjectClass objectClass,
            AnyTypeKind anyTypeKind,
            PlainSchema uidOnCreate,
            SyncopePullResultHandler handler,
            OperationOptions options) {

    }

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected PlainAttrValidationManager validator;

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected LiveSyncTaskExecSaver liveSyncTaskExecSaver;

    protected ProvisioningProfile<PullTask, InboundActions> profile;

    protected LiveSyncDeltaMapper mapper;

    protected List<LiveSyncInfo> infos;

    protected final Map<String, InboundActions> perContextActions = new ConcurrentHashMap<>();

    protected volatile boolean stopRequested = false;

    protected RealmPullResultHandler buildRealmHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultRealmPullResultHandler.class);
    }

    protected AnyObjectPullResultHandler buildAnyObjectHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultAnyObjectPullResultHandler.class);
    }

    protected UserPullResultHandler buildUserHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultUserPullResultHandler.class);
    }

    protected GroupPullResultHandler buildGroupHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultGroupPullResultHandler.class);
    }

    @Override
    protected void init(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context) throws JobExecutionException {

        super.init(taskType, taskKey, context);

        Implementation impl = Optional.ofNullable(task.getLiveSyncDeltaMapper()).
                orElseThrow(() -> new JobExecutionException(
                "No " + LiveSyncDeltaMapper.class.getSimpleName() + " provided, aborting"));
        try {
            mapper = ImplementationManager.build(impl);
        } catch (Exception e) {
            throw new JobExecutionException(
                    "Could not build " + IdMImplementationType.LIVE_SYNC_DELTA_MAPPER + " " + impl.getKey(), e);
        }

        PullTask pullTask = entityFactory.newEntity(PullTask.class);
        pullTask.setName(task.getName());
        pullTask.setResource(task.getResource());
        pullTask.setMatchingRule(task.getMatchingRule());
        pullTask.setUnmatchingRule(task.getUnmatchingRule());
        pullTask.setPullMode(PullMode.INCREMENTAL);
        pullTask.setPerformCreate(task.isPerformCreate());
        pullTask.setPerformUpdate(task.isPerformUpdate());
        pullTask.setPerformDelete(task.isPerformDelete());
        pullTask.setSyncStatus(task.isSyncStatus());
        pullTask.setDestinationRealm(task.getDestinationRealm());
        pullTask.setRemediation(task.isRemediation());
        task.getTemplates().forEach(atlst -> {
            AnyTemplatePullTask atpt = entityFactory.newEntity(AnyTemplatePullTask.class);
            atpt.setAnyType(atlst.getAnyType());
            atpt.setPullTask(pullTask);

            pullTask.add(atpt);
            atpt.set(atlst.get());
        });

        List<InboundActions> actions = new ArrayList<>();
        task.getActions().forEach(action -> {
            try {
                actions.add(ImplementationManager.build(
                        action,
                        () -> perContextActions.get(action.getKey()),
                        instance -> perContextActions.put(action.getKey(), instance)));
            } catch (Exception e) {
                LOG.warn("While building {}", action, e);
            }
        });
        profile = new ProvisioningProfile<>(
                connector,
                taskType,
                pullTask,
                Optional.ofNullable(task.getResource().getInboundPolicy()).
                        map(InboundPolicy::getConflictResolutionAction).
                        orElse(ConflictResolutionAction.IGNORE),
                actions,
                executor,
                context.isDryRun()) {

            @Override
            public String getContext() {
                return taskType + " Task " + taskKey + " '" + task.getName() + "'";
            }
        };

        infos = new ArrayList<>();

        // First realms...
        if (task.getResource().getOrgUnit() != null) {
            setStatus("Pulling " + task.getResource().getOrgUnit().getObjectClass());

            OrgUnit orgUnit = task.getResource().getOrgUnit();

            Set<String> moreAttrsToGet = new HashSet<>();
            profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, orgUnit)));
            OperationOptions options = MappingUtils.buildOperationOptions(
                    MappingUtils.getInboundItems(orgUnit.getItems().stream()), moreAttrsToGet.toArray(String[]::new));

            RealmPullResultHandler handler = buildRealmHandler();
            handler.setProfile(profile);

            infos.add(new LiveSyncInfo(
                    null,
                    orgUnit,
                    new ObjectClass(orgUnit.getObjectClass()),
                    null,
                    null,
                    handler,
                    options));
        }

        // ...then provisions for any types
        ProvisionSorter provisionSorter = getProvisionSorter(task);

        for (Provision provision : task.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).sorted(provisionSorter).
                toList()) {

            AnyType anyType = anyTypeDAO.findById(provision.getAnyType()).
                    orElseThrow(() -> new NotFoundException("AnyType" + provision.getAnyType()));

            PlainSchema uidOnCreate = null;
            if (provision.getUidOnCreate() != null) {
                uidOnCreate = plainSchemaDAO.findById(provision.getUidOnCreate()).
                        orElseThrow(() -> new NotFoundException("PlainSchema " + provision.getUidOnCreate()));
            }

            SyncopePullResultHandler handler;
            switch (anyType.getKind()) {
                case USER:
                    handler = buildUserHandler();
                    break;

                case GROUP:
                    handler = buildGroupHandler();
                    break;

                case ANY_OBJECT:
                default:
                    handler = buildAnyObjectHandler();
            }
            handler.setProfile(profile);

            Set<String> moreAttrsToGet = new HashSet<>();
            profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, provision)));
            Stream<Item> mapItems = Stream.concat(
                    MappingUtils.getInboundItems(provision.getMapping().getItems().stream()),
                    virSchemaDAO.findByResourceAndAnyType(
                            task.getResource().getKey(), anyType.getKey()).stream().
                            map(VirSchema::asLinkingMappingItem));
            OperationOptions options = MappingUtils.buildOperationOptions(
                    mapItems, moreAttrsToGet.toArray(String[]::new));

            infos.add(new LiveSyncInfo(
                    provision,
                    null,
                    new ObjectClass(provision.getObjectClass()),
                    anyType.getKind(),
                    uidOnCreate,
                    handler,
                    options));
        }

        setStatus("Initialization completed");
    }

    @Override
    public void stop() {
        stopRequested = true;
    }

    @Transactional
    @Override
    public void execute(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context) throws JobExecutionException {

        init(taskType, taskKey, context);

        setStatus("Initialization completed");

        doExecute(context);

        end();
    }

    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
        if (infos.isEmpty()) {
            LOG.info("Nothing to live sync on, aborting...");
            return StringUtils.EMPTY;
        }

        LOG.debug("Executing live sync on {}", task.getResource());

        while (!stopRequested) {
            profile.getResults().clear();

            TaskExec<SchedTask> execution = initExecution();
            execution.setTask(null);

            String message;
            String status;
            OpEvent.Outcome result;
            try {
                infos.forEach(info -> {
                    setStatus("Live syncing " + info.objectClass().getObjectClassValue());

                    profile.getConnector().livesync(
                            info.objectClass(),
                            liveSyncDelta -> {
                                try {
                                    LOG.debug("LiveSyncDelta: {}", liveSyncDelta);
                                    SyncDelta syncDelta = info.provision() == null
                                    ? mapper.map(liveSyncDelta, info.orgUnit())
                                    : mapper.map(liveSyncDelta, info.provision());
                                    LOG.debug("Mapped SyncDelta: {}", syncDelta);

                                    return info.handler().handle(syncDelta);
                                } catch (Exception e) {
                                    LOG.error("While live syncing from {} with {}",
                                            task.getResource().getKey(), liveSyncDelta, e);
                                    return false;
                                }
                            },
                            info.options());

                    if (info.uidOnCreate() != null) {
                        AnyUtils anyUtils = anyUtilsFactory.getInstance(info.anyTypeKind());
                        profile.getResults().stream().
                                filter(r -> r.getUidValue() != null && r.getKey() != null
                                && r.getOperation() == ResourceOperation.CREATE
                                && r.getAnyType().equals(info.provision().getAnyType())).
                                forEach(r -> anyUtils.addAttr(
                                validator,
                                r.getKey(),
                                info.uidOnCreate(),
                                r.getUidValue()));
                    }

                    if (info.anyTypeKind() == AnyTypeKind.GROUP) {
                        try {
                            PullJobDelegate.setGroupOwners(
                                    (GroupPullResultHandler) info.handler(),
                                    groupDAO,
                                    anyTypeDAO,
                                    inboundMatcher,
                                    profile);
                        } catch (Exception e) {
                            LOG.error("While setting group owners", e);
                        }
                    }
                });

                message = createReport(profile.getResults(), task.getResource(), context.isDryRun());
                status = TaskJob.Status.SUCCESS.name();
                result = OpEvent.Outcome.SUCCESS;
            } catch (Throwable t) {
                LOG.error("While executing task {}", task.getKey(), t);

                message = ExceptionUtils2.getFullStackTrace(t);
                status = TaskJob.Status.FAILURE.name();
                result = OpEvent.Outcome.FAILURE;
            }

            if (!profile.getResults().isEmpty()) {
                liveSyncTaskExecSaver.save(task.getKey(), execution, message, status, result, this::hasToBeRegistered);
            }
        }

        return StringUtils.EMPTY;
    }
}
