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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.sync.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.sync.SyncopePushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

    protected abstract String getName(Any<?, ?, ?> any);

    protected abstract Any<?, ?, ?> getAny(long key);

    protected abstract Any<?, ?, ?> deprovision(Any<?, ?, ?> sbj);

    protected abstract Any<?, ?, ?> provision(Any<?, ?, ?> sbj, Boolean enabled);

    protected abstract Any<?, ?, ?> link(Any<?, ?, ?> sbj, Boolean unlink);

    protected abstract Any<?, ?, ?> unassign(Any<?, ?, ?> sbj);

    protected abstract Any<?, ?, ?> assign(Any<?, ?, ?> sbj, Boolean enabled);

    protected abstract ConnectorObject getRemoteObject(String connObjectKey, ObjectClass objectClass);

    @Transactional
    @Override
    public boolean handle(final long anyKey) {
        Any<?, ?, ?> any = null;
        try {
            any = getAny(anyKey);
            doHandle(any);
            return true;
        } catch (IgnoreProvisionException e) {
            ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(any == null ? null : any.getType().getKey());
            result.setStatus(ProvisioningResult.Status.IGNORE);
            result.setKey(anyKey);
            profile.getResults().add(result);

            LOG.warn("Ignoring during push", e);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Push failed", e);
            return false;
        }
    }

    protected final void doHandle(final Any<?, ?, ?> any)
            throws JobExecutionException {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

        ProvisioningResult result = new ProvisioningResult();
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
        ConnectorObject beforeObj = null;
        String operation = null;

        // Try to read remote object (user / group) BEFORE any actual operation
        Provision provision = profile.getTask().getResource().getProvision(any.getType());
        String connObjecKey = MappingUtils.getConnObjectKeyValue(any, provision);

        beforeObj = getRemoteObject(connObjecKey, provision.getObjectClass());

        Boolean status = profile.getTask().isSyncStatus() ? enabled : null;

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(getResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningResult.Status.SUCCESS);
        } else {
            try {
                if (beforeObj == null) {
                    operation = UnmatchingRule.toEventName(profile.getTask().getUnmatchingRule());
                    result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(this.getProfile(), any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                assign(any, status);
                            }

                            break;

                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(this.getProfile(), any);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                provision(any, status);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), any);
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
                                action.beforeUpdate(this.getProfile(), any);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                update(any, status);
                            }

                            break;

                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(this.getProfile(), any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                deprovision(any);
                            }

                            break;

                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(this.getProfile(), any);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                unassign(any);
                            }

                            break;

                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(this.getProfile(), any);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(any, false);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), any);
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
                    action.after(this.getProfile(), any, result);
                }

                result.setStatus(ProvisioningResult.Status.SUCCESS);
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(connObjecKey, provision.getObjectClass());
            } catch (IgnoreProvisionException e) {
                throw e;
            } catch (Exception e) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                resultStatus = AuditElements.Result.FAILURE;
                output = e;

                LOG.warn("Error pushing {} towards {}", any, profile.getTask().getResource(), e);

                for (PushActions action : profile.getActions()) {
                    action.onError(this.getProfile(), any, result, e);
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

    protected Any<?, ?, ?> update(final Any<?, ?, ?> sbj, final Boolean enabled) {
        Set<String> vattrToBeRemoved = new HashSet<>();
        Set<AttrMod> vattrToBeUpdated = new HashSet<>();

        // Search for all mapped vattrs
        Mapping mapping = profile.getTask().getResource().getProvision(sbj.getType()).getMapping();
        for (MappingItem mappingItem : mapping.getItems()) {
            if (mappingItem.getIntMappingType() == IntMappingType.UserVirtualSchema) {
                vattrToBeRemoved.add(mappingItem.getIntAttrName());
            }
        }

        // Search for all user's vattrs and:
        // 1. add mapped vattrs not owned by the user to the set of vattrs to be removed
        // 2. add all vattrs owned by the user to the set of vattrs to be update
        for (VirAttr<?> vattr : sbj.getVirAttrs()) {
            vattrToBeRemoved.remove(vattr.getSchema().getKey());
            AttrMod mod = new AttrMod();
            mod.setSchema(vattr.getSchema().getKey());
            mod.getValuesToBeAdded().addAll(vattr.getValues());
            vattrToBeUpdated.add(mod);
        }

        boolean changepwd;
        Collection<String> resourceNames;
        if (sbj instanceof User) {
            changepwd = true;
            resourceNames = userDAO.findAllResourceNames((User) sbj);
        } else if (sbj instanceof AnyObject) {
            changepwd = true;
            resourceNames = anyObjectDAO.findAllResourceNames((AnyObject) sbj);
        } else {
            changepwd = false;
            resourceNames = ((Group) sbj).getResourceNames();
        }

        List<String> noPropResources = new ArrayList<>(resourceNames);
        noPropResources.remove(profile.getTask().getResource().getKey());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUpdateTasks(
                sbj, null, changepwd, enabled, vattrToBeRemoved, vattrToBeUpdated, propByRes, noPropResources));

        return getAny(sbj.getKey());
    }
}
