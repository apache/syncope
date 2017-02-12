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
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
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
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

    @Autowired
    protected MappingManager mappingManager;

    protected abstract String getName(Any<?> any);

    protected void deprovision(final Any<?> any) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getDeleteTasks(
                any.getType().getKind(),
                any.getKey(),
                null,
                noPropResources));
    }

    protected void provision(final Any<?> any, final Boolean enabled) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getCreateTasks(
                any.getType().getKind(),
                any.getKey(),
                propByRes,
                before.getVirAttrs(),
                noPropResources));
    }

    protected void link(final Any<?> any, final Boolean unlink) {
        AnyPatch patch = newPatch(any.getKey());
        patch.getResources().add(new StringPatchItem.Builder().
                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        update(patch);
    }

    protected void unassign(final Any<?> any) {
        AnyPatch patch = newPatch(any.getKey());
        patch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.DELETE).
                value(profile.getTask().getResource().getKey()).build());

        update(patch);

        deprovision(any);
    }

    protected void assign(final Any<?> any, final Boolean enabled) {
        AnyPatch patch = newPatch(any.getKey());
        patch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        update(patch);

        provision(any, enabled);
    }

    protected ConnectorObject getRemoteObject(final String connObjectKey, final ObjectClass objectClass) {
        ConnectorObject obj = null;
        try {
            Uid uid = new Uid(connObjectKey);

            obj = profile.getConnector().getObject(objectClass,
                    uid,
                    MappingUtils.buildOperationOptions(IteratorUtils.<MappingItem>emptyIterator()));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", connObjectKey, ignore);
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
        String operation = null;

        // Try to read remote object BEFORE any actual operation
        Provision provision = profile.getTask().getResource().getProvision(any.getType());
        String connObjecKey = mappingManager.getConnObjectKeyValue(any, provision);

        ConnectorObject beforeObj = getRemoteObject(connObjecKey, provision.getObjectClass());

        Boolean status = profile.getTask().isSyncStatus() ? enabled : null;

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(getResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningReport.Status.SUCCESS);
        } else {
            try {
                if (beforeObj == null) {
                    operation = UnmatchingRule.toEventName(profile.getTask().getUnmatchingRule());
                    result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(profile, any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                assign(any, status);
                            }

                            break;

                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(profile, any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                provision(any, status);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(any, true);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", any);
                            break;
                        default:
                        // do nothing
                    }
                } else {
                    operation = MatchingRule.toEventName(profile.getTask().getMatchingRule());
                    result.setOperation(getResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(profile, any);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                update(any);
                            }

                            break;

                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                deprovision(any);
                            }

                            break;

                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(profile, any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                unassign(any);
                            }

                            break;

                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(any, false);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(any, true);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", any);
                            break;
                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(profile, any, result);
                }

                result.setStatus(ProvisioningReport.Status.SUCCESS);
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(connObjecKey, provision.getObjectClass());
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
                notificationManager.createTasks(AuditElements.EventCategoryType.PUSH,
                        any.getType().getKind().name().toLowerCase(),
                        profile.getTask().getResource().getKey(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        any);
                auditManager.audit(AuditElements.EventCategoryType.PUSH,
                        any.getType().getKind().name().toLowerCase(),
                        profile.getTask().getResource().getKey(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        any);
            }
        }
    }

    private ResourceOperation getResourceOperation(final UnmatchingRule rule) {
        switch (rule) {
            case ASSIGN:
            case PROVISION:
                return ResourceOperation.CREATE;
            default:
                return ResourceOperation.NONE;
        }
    }

    private ResourceOperation getResourceOperation(final MatchingRule rule) {
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

    private Any<?> update(final Any<?> any) {
        boolean changepwd;
        Collection<String> resourceKeys;
        if (any instanceof User) {
            changepwd = true;
            resourceKeys = CollectionUtils.collect(
                    userDAO.findAllResources((User) any), EntityUtils.keyTransformer());
        } else if (any instanceof AnyObject) {
            changepwd = false;
            resourceKeys = CollectionUtils.collect(
                    anyObjectDAO.findAllResources((AnyObject) any), EntityUtils.keyTransformer());
        } else {
            changepwd = false;
            resourceKeys = ((Group) any).getResourceKeys();
        }

        List<String> noPropResources = new ArrayList<>(resourceKeys);
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUpdateTasks(
                any.getType().getKind(),
                any.getKey(),
                changepwd,
                null,
                propByRes,
                null,
                noPropResources));

        return getAny(any.getKey());
    }
}
