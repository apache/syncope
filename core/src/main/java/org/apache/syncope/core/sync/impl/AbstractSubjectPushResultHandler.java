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
package org.apache.syncope.core.sync.impl;

import static org.apache.syncope.core.sync.impl.AbstractSyncopeResultHandler.LOG;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MatchingRule;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.UnmatchingRule;
import org.apache.syncope.core.persistence.beans.AbstractMapping;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.sync.PushActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractSubjectPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions> {

    protected abstract String getName(final AbstractSubject subject);

    protected abstract AbstractMapping getMapping();

    protected abstract AbstractSubjectTO getSubjectTO(final long id);

    protected abstract AbstractSubject getSubject(final long id);

    protected abstract AbstractSubject deprovision(final AbstractSubject sbj);

    protected abstract AbstractSubject provision(final AbstractSubject sbj, final Boolean enabled);

    protected abstract AbstractSubject link(final AbstractSubject sbj, final Boolean unlink);

    protected abstract AbstractSubject unassign(final AbstractSubject sbj);

    protected abstract AbstractSubject assign(final AbstractSubject sbj, Boolean enabled);

    protected abstract ConnectorObject getRemoteObject(final String accountId);

    @Transactional
    public boolean handle(final AbstractSubject subject) {
        try {
            doHandle(subject);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    protected final void doHandle(final AbstractSubject subject)
            throws JobExecutionException {

        if (profile.getResults() == null) {
            profile.setResults(new ArrayList<SyncResult>());
        }

        final AbstractSubject toBeHandled = getSubject(subject.getId());

        final AttributableUtil attrUtil = AttributableUtil.getInstance(toBeHandled);

        final SyncResult result = new SyncResult();
        profile.getResults().add(result);

        result.setId(toBeHandled.getId());
        result.setSubjectType(attrUtil.getType());
        result.setName(getName(toBeHandled));

        final Boolean enabled = toBeHandled instanceof SyncopeUser && profile.getSyncTask().isSyncStatus()
                ? ((SyncopeUser) toBeHandled).isSuspended() ? Boolean.FALSE : Boolean.TRUE
                : null;

        LOG.debug("Propagating {} with ID {} towards {}",
                attrUtil.getType(), toBeHandled.getId(), profile.getSyncTask().getResource());

        Object output = null;
        Result resultStatus = null;
        ConnectorObject beforeObj = null;
        String operation = null;

        // Try to read remote object (user / group) BEFORE any actual operation

        final String accountId = MappingUtil.getAccountIdValue(
                subject, profile.getSyncTask().getResource(), getMapping().getAccountIdItem());

        beforeObj = getRemoteObject(accountId);

        Boolean status = profile.getSyncTask().isSyncStatus() ? enabled : null;

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(getResourceOperation(profile.getSyncTask().getUnmatchingRule()));
            } else {
                result.setOperation(getResourceOperation(profile.getSyncTask().getMatchingRule()));
            }
            result.setStatus(SyncResult.Status.SUCCESS);
        } else {
            try {
                if (beforeObj == null) {
                    operation = profile.getSyncTask().getUnmatchingRule().name().toLowerCase();
                    result.setOperation(getResourceOperation(profile.getSyncTask().getUnmatchingRule()));

                    switch (profile.getSyncTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                assign(toBeHandled, status);
                            }

                            break;
                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                provision(toBeHandled, status);
                            }

                            break;
                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(toBeHandled, true);
                            }

                            break;
                        default:
                        // do nothing
                    }

                } else {
                    operation = profile.getSyncTask().getMatchingRule().name().toLowerCase();
                    result.setOperation(getResourceOperation(profile.getSyncTask().getMatchingRule()));

                    switch (profile.getSyncTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(this.getProfile(), toBeHandled);
                            }
                            if (!profile.getSyncTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                update(toBeHandled, status);
                            }

                            break;
                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                deprovision(toBeHandled);
                            }

                            break;
                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                unassign(toBeHandled);
                            }

                            break;
                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(toBeHandled, false);
                            }

                            break;
                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), toBeHandled);
                            }

                            if (!profile.getSyncTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(toBeHandled, true);
                            }

                            break;
                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(this.getProfile(), toBeHandled, result);
                }

                result.setStatus(SyncResult.Status.SUCCESS);
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(accountId);
            } catch (Exception e) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                resultStatus = AuditElements.Result.FAILURE;
                output = e;

                LOG.warn("Error pushing {} towards {}", toBeHandled, profile.getSyncTask().getResource(), e);
                throw new JobExecutionException(e);
            } finally {
                notificationManager.createTasks(
                        AuditElements.EventCategoryType.PUSH,
                        AttributableType.USER.name().toLowerCase(),
                        profile.getSyncTask().getResource().getName(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        toBeHandled);
                auditManager.audit(
                        AuditElements.EventCategoryType.PUSH,
                        AttributableType.USER.name().toLowerCase(),
                        profile.getSyncTask().getResource().getName(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        toBeHandled);
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

    protected AbstractSubject update(final AbstractSubject sbj, final Boolean enabled) {

        final Set<MembershipMod> membsToAdd = new HashSet<MembershipMod>();
        final Set<String> vattrToBeRemoved = new HashSet<String>();
        final Set<String> membVattrToBeRemoved = new HashSet<String>();
        final Set<AttributeMod> vattrToBeUpdated = new HashSet<AttributeMod>();

        // Search for all mapped vattrs
        final AbstractMapping umapping = getMapping();
        for (AbstractMappingItem mappingItem : umapping.getItems()) {
            if (mappingItem.getIntMappingType() == IntMappingType.UserVirtualSchema) {
                vattrToBeRemoved.add(mappingItem.getIntAttrName());
            } else if (mappingItem.getIntMappingType() == IntMappingType.MembershipVirtualSchema) {
                membVattrToBeRemoved.add(mappingItem.getIntAttrName());
            }
        }

        // Search for all user's vattrs and:
        // 1. add mapped vattrs not owned by the user to the set of vattrs to be removed
        // 2. add all vattrs owned by the user to the set of vattrs to be update
        for (AbstractVirAttr vattr : sbj.getVirAttrs()) {
            vattrToBeRemoved.remove(vattr.getSchema().getName());
            final AttributeMod mod = new AttributeMod();
            mod.setSchema(vattr.getSchema().getName());
            mod.getValuesToBeAdded().addAll(vattr.getValues());
            vattrToBeUpdated.add(mod);
        }

        final boolean changepwd;

        if (sbj instanceof SyncopeUser) {
            changepwd = true;

            // Search for memberships
            for (Membership membership : SyncopeUser.class.cast(sbj).getMemberships()) {
                final MembershipMod membershipMod = new MembershipMod();
                membershipMod.setId(membership.getId());
                membershipMod.setRole(membership.getSyncopeRole().getId());

                for (AbstractVirAttr vattr : membership.getVirAttrs()) {
                    membVattrToBeRemoved.remove(vattr.getSchema().getName());
                    final AttributeMod mod = new AttributeMod();
                    mod.setSchema(vattr.getSchema().getName());
                    mod.getValuesToBeAdded().addAll(vattr.getValues());
                    membershipMod.getVirAttrsToUpdate().add(mod);
                }

                membsToAdd.add(membershipMod);
            }

            if (!membsToAdd.isEmpty()) {
                membsToAdd.iterator().next().getVirAttrsToRemove().addAll(membVattrToBeRemoved);
            }
        } else {
            changepwd = false;
        }

        final List<String> noPropResources = new ArrayList<String>(sbj.getResourceNames());
        noPropResources.remove(profile.getSyncTask().getResource().getName());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getSyncTask().getResource().getName());

        taskExecutor.execute(propagationManager.getUpdateTaskIds(
                sbj, null, changepwd, enabled, vattrToBeRemoved, vattrToBeUpdated, propByRes, noPropResources,
                membsToAdd));

        return userDataBinder.getUserFromId(sbj.getId());
    }
}
