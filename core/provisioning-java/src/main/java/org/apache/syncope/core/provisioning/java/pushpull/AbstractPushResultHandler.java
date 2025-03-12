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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.java.job.AfterHandlingJob;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationReporter;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

    protected static void reportPropagation(final ProvisioningReport result, final PropagationReporter reporter) {
        if (!reporter.getStatuses().isEmpty()) {
            result.setStatus(toProvisioningReportStatus(reporter.getStatuses().getFirst().getStatus()));
            result.setMessage(reporter.getStatuses().getFirst().getFailureReason());
        }
    }

    protected static ResourceOperation toResourceOperation(final UnmatchingRule rule) {
        return switch (rule) {
            case ASSIGN, PROVISION ->
                ResourceOperation.CREATE;
            default ->
                ResourceOperation.NONE;
        };
    }

    protected static ResourceOperation toResourceOperation(final MatchingRule rule) {
        return switch (rule) {
            case UPDATE ->
                ResourceOperation.UPDATE;
            case DEPROVISION, UNASSIGN ->
                ResourceOperation.DELETE;
            default ->
                ResourceOperation.NONE;
        };
    }

    protected static ProvisioningReport.Status toProvisioningReportStatus(final ExecStatus status) {
        switch (status) {
            case FAILURE:
                return ProvisioningReport.Status.FAILURE;

            case SUCCESS:
                return ProvisioningReport.Status.SUCCESS;

            case CREATED:
            case NOT_ATTEMPTED:
            default:
                return ProvisioningReport.Status.IGNORE;
        }
    }

    @Autowired
    protected OutboundMatcher outboundMatcher;

    /**
     * Notification Manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit Manager.
     */
    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected MappingManager mappingManager;

    @Autowired
    protected SyncopeTaskScheduler scheduler;

    protected abstract String getName(Any any);

    protected void update(
            final Any any,
            final Boolean enable,
            final ConnectorObject beforeObj,
            final ProvisioningReport result) {
        
        List<String> ownedResources = getAnyUtils().getAllResources(any).stream().
                map(ExternalResource::getKey).toList();

        List<String> noPropResources = new ArrayList<>(ownedResources);
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.UPDATE, profile.getTask().getResource().getKey());
        propByRes.addOldConnObjectKey(profile.getTask().getResource().getKey(), beforeObj.getUid().getUidValue());

        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                null,
                any.getType().getKind(),
                any.getKey(),
                any instanceof User ? List.of(profile.getTask().getResource().getKey()) : List.of(),
                enable,
                propByRes,
                null,
                null,
                noPropResources);
        if (!taskInfos.isEmpty()) {
            taskInfos.getFirst().setBeforeObj(Optional.of(beforeObj));
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, securityProperties.getAdminUser());
            reportPropagation(result, reporter);
        }
    }

    protected void deprovision(final Any any, final ConnectorObject beforeObj, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any);

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());
        propByRes.addOldConnObjectKey(profile.getTask().getResource().getKey(), beforeObj.getUid().getUidValue());

        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                any.getType().getKind(),
                any.getKey(),
                propByRes,
                null,
                noPropResources);
        if (!taskInfos.isEmpty()) {
            taskInfos.getFirst().setBeforeObj(Optional.of(beforeObj));
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, securityProperties.getAdminUser());
            reportPropagation(result, reporter);
        }
    }

    protected void provision(final Any any, final Boolean enable, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any);

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        List<PropagationTaskInfo> taskInfos = propagationManager.getCreateTasks(
                any.getType().getKind(),
                any.getKey(),
                enable,
                propByRes,
                before.getVirAttrs(),
                noPropResources);
        if (!taskInfos.isEmpty()) {
            taskInfos.getFirst().setBeforeObj(Optional.empty());
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, securityProperties.getAdminUser());
            reportPropagation(result, reporter);
        }
    }

    protected void link(final Any any, final boolean unlink, final ProvisioningReport result) {
        AnyUR req = getAnyUtils().newAnyUR(any.getKey());
        req.getResources().add(new StringPatchItem.Builder().
                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        update(req);

        result.setStatus(ProvisioningReport.Status.SUCCESS);
    }

    protected void unassign(final Any any, final ConnectorObject beforeObj, final ProvisioningReport result) {
        AnyUR req = getAnyUtils().newAnyUR(any.getKey());
        req.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.DELETE).
                value(profile.getTask().getResource().getKey()).build());

        update(req);

        deprovision(any, beforeObj, result);
    }

    protected void assign(final Any any, final Boolean enabled, final ProvisioningReport result) {
        AnyUR req = getAnyUtils().newAnyUR(any.getKey());
        req.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        update(req);

        provision(any, enabled, result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean handle(final String anyKey) {
        Any any = null;
        try {
            any = getAnyUtils().dao().authFind(anyKey);

            Provision provision = profile.getTask().getResource().
                    getProvisionByAnyType(any.getType().getKey()).orElse(null);
            if (provision == null) {
                throw new JobExecutionException("No provision found on " + profile.getTask().getResource() + " for "
                        + any.getType().getKey());
            }

            doHandle(any, provision);

            if (stopRequested) {
                LOG.debug("Stop was requested");
                return false;
            }

            return true;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport ignoreResult = profile.getResults().stream().
                    filter(report -> anyKey.equalsIgnoreCase(report.getKey())).
                    findFirst().
                    orElse(null);
            if (ignoreResult == null) {
                ignoreResult = new ProvisioningReport();
                ignoreResult.setKey(anyKey);
                ignoreResult.setAnyType(Optional.ofNullable(any).map(any1 -> any1.getType().getKey()).orElse(null));

                profile.getResults().add(ignoreResult);
            }

            ignoreResult.setOperation(ResourceOperation.NONE);
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setMessage(e.getMessage());

            LOG.warn("Ignoring during push", e);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Push failed", e);
            return false;
        }
    }

    protected void doHandle(final Any any, final Provision provision) throws JobExecutionException {
        ProvisioningReport result = new ProvisioningReport();
        profile.getResults().add(result);

        result.setKey(any.getKey());
        result.setAnyType(any.getType().getKey());
        result.setName(getName(any));

        LOG.debug("Pushing {} with key {} towards {}",
                any.getType().getKind(), any.getKey(), profile.getTask().getResource());

        // Try to read remote object BEFORE any actual operation
        Set<String> moreAttrsToGet = new HashSet<>();
        profile.getActions().forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(profile, any)));
        List<ConnectorObject> connObjs = outboundMatcher.match(
                profile.getConnector(),
                any,
                profile.getTask().getResource(),
                provision,
                Optional.of(moreAttrsToGet.toArray(String[]::new)));
        LOG.debug("Match(es) found for {} as {}: {}", any, provision.getObjectClass(), connObjs);

        if (connObjs.size() > 1) {
            switch (profile.getConflictResolutionAction()) {
                case IGNORE:
                    throw new IgnoreProvisionException("More than one match found for "
                            + any.getKey() + ": " + connObjs);

                case FIRSTMATCH:
                    connObjs = connObjs.subList(0, 1);
                    break;

                case LASTMATCH:
                    connObjs = connObjs.subList(connObjs.size() - 1, connObjs.size());
                    break;

                default:
            }
        }
        ConnectorObject beforeObj = connObjs.isEmpty() ? null : connObjs.getFirst();

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningReport.Status.SUCCESS);
        } else {
            String operation = beforeObj == null
                    ? UnmatchingRule.toOp(profile.getTask().getUnmatchingRule())
                    : MatchingRule.toOp(profile.getTask().getMatchingRule());

            boolean notificationsAvailable = notificationManager.notificationsAvailable(
                    AuthContextUtils.getDomain(),
                    OpEvent.CategoryType.PUSH,
                    any.getType().getKind().name(),
                    profile.getTask().getResource().getKey(),
                    operation);
            boolean auditRequested = auditManager.auditRequested(
                    AuthContextUtils.getDomain(),
                    AuthContextUtils.getUsername(),
                    OpEvent.CategoryType.PUSH,
                    any.getType().getKind().name(),
                    profile.getTask().getResource().getKey(),
                    operation);

            Object output = null;
            OpEvent.Outcome resultStatus = null;

            Boolean enable = any instanceof User user && profile.getTask().isSyncStatus()
                    ? BooleanUtils.negate(user.isSuspended())
                    : null;
            try {
                if (beforeObj == null) {
                    result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(profile, any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                assign(any, enable, result);
                            }
                        }

                        case PROVISION -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(profile, any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                provision(any, enable, result);
                            }
                        }

                        case UNLINK -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(any, true, result);
                            }
                        }

                        case IGNORE -> {
                            LOG.debug("Ignored any: {}", any);
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                        }

                        default -> {
                        }
                    }
                    // do nothing
                } else {
                    result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(profile, any);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                update(any, enable, beforeObj, result);
                            }
                        }

                        case DEPROVISION -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                deprovision(any, beforeObj, result);
                            }
                        }

                        case UNASSIGN -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(profile, any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                unassign(any, beforeObj, result);
                            }
                        }

                        case LINK -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(any, false, result);
                            }
                        }

                        case UNLINK -> {
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(any, true, result);
                            }
                        }

                        case IGNORE -> {
                            LOG.debug("Ignored any: {}", any);
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                        }

                        default -> {
                        }
                    }
                    // do nothing
                }

                for (PushActions action : profile.getActions()) {
                    action.after(profile, any, result);
                }

                if (result.getStatus() == null) {
                    result.setStatus(ProvisioningReport.Status.SUCCESS);
                }

                if (notificationsAvailable || auditRequested) {
                    resultStatus = OpEvent.Outcome.SUCCESS;
                    output = outboundMatcher.match(
                            profile.getConnector(),
                            any,
                            profile.getTask().getResource(),
                            provision,
                            Optional.of(moreAttrsToGet.toArray(String[]::new)));
                }
            } catch (IgnoreProvisionException e) {
                throw e;
            } catch (Exception e) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));

                if (notificationsAvailable || auditRequested) {
                    resultStatus = OpEvent.Outcome.FAILURE;
                    output = e;
                }

                LOG.warn("Error pushing {} towards {}", any, profile.getTask().getResource(), e);

                for (PushActions action : profile.getActions()) {
                    action.onError(profile, any, result, e);
                }

                throw new JobExecutionException(e);
            } finally {
                if (notificationsAvailable || auditRequested) {
                    Map<String, Object> jobMap = new HashMap<>();
                    jobMap.put(AfterHandlingEvent.JOBMAP_KEY, new AfterHandlingEvent(
                            AuthContextUtils.getDomain(),
                            AuthContextUtils.getWho(),
                            OpEvent.CategoryType.PUSH,
                            any.getType().getKind().name(),
                            profile.getTask().getResource().getKey(),
                            operation,
                            resultStatus,
                            beforeObj,
                            output,
                            any));
                    AfterHandlingJob.schedule(scheduler, jobMap);
                }
            }
        }
    }
}
