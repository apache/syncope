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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.java.job.AfterHandlingJob;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

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
    protected SchedulerFactoryBean scheduler;

    protected abstract String getName(Any<?> any);

    protected void reportPropagation(final ProvisioningReport result, final PropagationReporter reporter) {
        if (!reporter.getStatuses().isEmpty()) {
            result.setStatus(toProvisioningReportStatus(reporter.getStatuses().get(0).getStatus()));
            result.setMessage(reporter.getStatuses().get(0).getFailureReason());
        }
    }

    private void update(final Any<?> any, final ProvisioningReport result) {
        boolean changepwd;
        Collection<String> resourceKeys;
        if (any instanceof User) {
            changepwd = true;
            resourceKeys = userDAO.findAllResourceKeys(any.getKey());
        } else if (any instanceof AnyObject) {
            changepwd = false;
            resourceKeys = anyObjectDAO.findAllResourceKeys(any.getKey());
        } else {
            changepwd = false;
            resourceKeys = groupDAO.findAllResourceKeys(any.getKey());
        }

        List<String> noPropResources = new ArrayList<>(resourceKeys);
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        PropagationReporter reporter = taskExecutor.execute(propagationManager.getUpdateTasks(
                any.getType().getKind(),
                any.getKey(),
                changepwd,
                null,
                propByRes,
                null,
                noPropResources),
                false);
        reportPropagation(result, reporter);
    }

    protected void deprovision(final Any<?> any, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationReporter reporter = taskExecutor.execute(propagationManager.getDeleteTasks(
                any.getType().getKind(),
                any.getKey(),
                null,
                noPropResources),
                false);
        reportPropagation(result, reporter);
    }

    protected void provision(final Any<?> any, final Boolean enabled, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        PropagationReporter reporter = taskExecutor.execute(propagationManager.getCreateTasks(
                any.getType().getKind(),
                any.getKey(),
                propByRes,
                before.getVirAttrs(),
                noPropResources),
                false);
        reportPropagation(result, reporter);
    }

    protected void link(final Any<?> any, final boolean unlink, final ProvisioningReport result) {
        AnyPatch patch = newPatch(any.getKey());
        patch.getResources().add(new StringPatchItem.Builder().
                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        update(patch);

        result.setStatus(ProvisioningReport.Status.SUCCESS);
    }

    protected void unassign(final Any<?> any, final ProvisioningReport result) {
        AnyPatch patch = newPatch(any.getKey());
        patch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.DELETE).
                value(profile.getTask().getResource().getKey()).build());

        update(patch);

        deprovision(any, result);
    }

    protected void assign(final Any<?> any, final Boolean enabled, final ProvisioningReport result) {
        AnyPatch patch = newPatch(any.getKey());
        patch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        update(patch);

        provision(any, enabled, result);
    }

    protected ConnectorObject getRemoteObject(
            final ObjectClass objectClass,
            final String connObjectKey,
            final String connObjectKeyValue,
            final Iterator<? extends Item> iterator) {

        ConnectorObject obj = null;
        try {
            obj = profile.getConnector().getObject(
                    objectClass,
                    AttributeBuilder.build(connObjectKey, connObjectKeyValue),
                    MappingUtils.buildOperationOptions(iterator));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", connObjectKeyValue, ignore);
        }

        return obj;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean handle(final String anyKey) {
        Any<?> any = null;
        try {
            any = getAny(anyKey);
            doHandle(any);
            return true;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(any == null ? null : any.getType().getKey());
            result.setStatus(ProvisioningReport.Status.IGNORE);
            result.setKey(anyKey);
            profile.getResults().add(result);

            LOG.warn("Ignoring during push", e);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Push failed", e);
            return false;
        }
    }

    private void doHandle(final Any<?> any) throws JobExecutionException {
        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

        ProvisioningReport result = new ProvisioningReport();
        profile.getResults().add(result);

        result.setKey(any.getKey());
        result.setAnyType(any.getType().getKey());
        result.setName(getName(any));

        Boolean enabled = any instanceof User && profile.getTask().isSyncStatus()
                ? ((User) any).isSuspended() ? Boolean.FALSE : Boolean.TRUE
                : null;

        LOG.debug("Propagating {} with key {} towards {}",
                anyUtils.getAnyTypeKind(), any.getKey(), profile.getTask().getResource());

        Object output = null;
        Result resultStatus = null;

        // Try to read remote object BEFORE any actual operation
        Optional<? extends Provision> provision = profile.getTask().getResource().getProvision(any.getType());
        Optional<MappingItem> connObjectKey = MappingUtils.getConnObjectKeyItem(provision.get());
        Optional<String> connObjecKeyValue = mappingManager.getConnObjectKeyValue(any, provision.get());

        ConnectorObject beforeObj = getRemoteObject(
                provision.get().getObjectClass(),
                connObjectKey.get().getExtAttrName(),
                connObjecKeyValue.get(),
                provision.get().getMapping().getItems().iterator());

        Boolean status = profile.getTask().isSyncStatus() ? enabled : null;

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningReport.Status.SUCCESS);
        } else {
            String operation = beforeObj == null
                    ? UnmatchingRule.toEventName(profile.getTask().getUnmatchingRule())
                    : MatchingRule.toEventName(profile.getTask().getMatchingRule());

            boolean notificationsAvailable = notificationManager.notificationsAvailable(
                    AuditElements.EventCategoryType.PUSH,
                    any.getType().getKind().name().toLowerCase(),
                    profile.getTask().getResource().getKey(),
                    operation);
            boolean auditRequested = auditManager.auditRequested(
                    AuditElements.EventCategoryType.PUSH,
                    any.getType().getKind().name().toLowerCase(),
                    profile.getTask().getResource().getKey(),
                    operation);
            try {
                if (beforeObj == null) {
                    result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(profile, any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                assign(any, status, result);
                            }
                            break;

                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(profile, any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                provision(any, status, result);
                            }
                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(any, true, result);
                            }
                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", any);
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(profile, any);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                update(any, result);
                            }
                            break;

                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                deprovision(any, result);
                            }
                            break;

                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(profile, any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                unassign(any, result);
                            }
                            break;

                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(any, false, result);
                            }
                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(any, true, result);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", any);
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                            break;

                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(profile, any, result);
                }

                if (result.getStatus() == null) {
                    result.setStatus(ProvisioningReport.Status.SUCCESS);
                }
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(
                        provision.get().getObjectClass(),
                        connObjectKey.get().getExtAttrName(),
                        connObjecKeyValue.get(),
                        provision.get().getMapping().getItems().iterator());
            } catch (IgnoreProvisionException e) {
                throw e;
            } catch (Exception e) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                resultStatus = AuditElements.Result.FAILURE;
                output = e;

                LOG.warn("Error pushing {} towards {}", any, profile.getTask().getResource(), e);

                for (PushActions action : profile.getActions()) {
                    action.onError(profile, any, result, e);
                }

                throw new JobExecutionException(e);
            } finally {
                if (notificationsAvailable || auditRequested) {
                    Map<String, Object> jobMap = new HashMap<>();
                    jobMap.put(AfterHandlingEvent.JOBMAP_KEY, new AfterHandlingEvent(
                            AuditElements.EventCategoryType.PUSH,
                            any.getType().getKind().name().toLowerCase(),
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

    private ResourceOperation toResourceOperation(final UnmatchingRule rule) {
        switch (rule) {
            case ASSIGN:
            case PROVISION:
                return ResourceOperation.CREATE;
            default:
                return ResourceOperation.NONE;
        }
    }

    private ResourceOperation toResourceOperation(final MatchingRule rule) {
        switch (rule) {
            case UPDATE:
                return ResourceOperation.UPDATE;
            case DEPROVISION:
            case UNASSIGN:
                return ResourceOperation.DELETE;
            default:
                return ResourceOperation.NONE;
        }
    }

    private ProvisioningReport.Status toProvisioningReportStatus(final PropagationTaskExecStatus status) {
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
}
