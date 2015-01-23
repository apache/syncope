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
package org.apache.syncope.server.provisioning.java.sync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.MembershipMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.server.persistence.api.entity.AttributableUtil;
import org.apache.syncope.server.persistence.api.entity.Mapping;
import org.apache.syncope.server.persistence.api.entity.MappingItem;
import org.apache.syncope.server.persistence.api.entity.Subject;
import org.apache.syncope.server.persistence.api.entity.VirAttr;
import org.apache.syncope.server.persistence.api.entity.membership.Membership;
import org.apache.syncope.server.persistence.api.entity.task.PushTask;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.server.provisioning.api.sync.PushActions;
import org.apache.syncope.server.misc.MappingUtil;
import org.apache.syncope.server.provisioning.api.sync.SyncopePushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

    protected abstract String getName(final Subject<?, ?, ?> subject);

    protected abstract Mapping<?> getMapping();

    protected abstract AbstractSubjectTO getSubjectTO(final long key);

    protected abstract Subject<?, ?, ?> getSubject(final long key);

    protected abstract Subject<?, ?, ?> deprovision(final Subject<?, ?, ?> sbj);

    protected abstract Subject<?, ?, ?> provision(final Subject<?, ?, ?> sbj, final Boolean enabled);

    protected abstract Subject<?, ?, ?> link(final Subject<?, ?, ?> sbj, final Boolean unlink);

    protected abstract Subject<?, ?, ?> unassign(final Subject<?, ?, ?> sbj);

    protected abstract Subject<?, ?, ?> assign(final Subject<?, ?, ?> sbj, Boolean enabled);

    protected abstract ConnectorObject getRemoteObject(final String accountId);

    @Transactional
    @Override
    public boolean handle(final long subjectId) {
        try {
            doHandle(subjectId);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    protected final void doHandle(final long subjectId)
            throws JobExecutionException {

        final Subject<?, ?, ?> subject = getSubject(subjectId);

        final AttributableUtil attrUtil = attrUtilFactory.getInstance(subject);

        final ProvisioningResult result = new ProvisioningResult();
        profile.getResults().add(result);

        result.setId(subject.getKey());
        result.setSubjectType(attrUtil.getType());
        result.setName(getName(subject));

        final Boolean enabled = subject instanceof User && profile.getTask().isSyncStatus()
                ? ((User) subject).isSuspended() ? Boolean.FALSE : Boolean.TRUE
                : null;

        LOG.debug("Propagating {} with key {} towards {}",
                attrUtil.getType(), subject.getKey(), profile.getTask().getResource());

        Object output = null;
        Result resultStatus = null;
        ConnectorObject beforeObj = null;
        String operation = null;

        // Try to read remote object (user / group) BEFORE any actual operation
        final String accountId = MappingUtil.getAccountIdValue(
                subject, profile.getTask().getResource(), getMapping().getAccountIdItem());

        beforeObj = getRemoteObject(accountId);

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
                    operation = profile.getTask().getUnmatchingRule().name().toLowerCase();
                    result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                assign(subject, status);
                            }

                            break;
                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                provision(subject, status);
                            }

                            break;
                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(subject, true);
                            }

                            break;
                        default:
                        // do nothing
                    }

                } else {
                    operation = profile.getTask().getMatchingRule().name().toLowerCase();
                    result.setOperation(getResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(this.getProfile(), subject);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                update(subject, status);
                            }

                            break;
                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                deprovision(subject);
                            }

                            break;
                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                unassign(subject);
                            }

                            break;
                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(subject, false);
                            }

                            break;
                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(this.getProfile(), subject);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(subject, true);
                            }

                            break;
                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(this.getProfile(), subject, result);
                }

                result.setStatus(ProvisioningResult.Status.SUCCESS);
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(accountId);
            } catch (Exception e) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                resultStatus = AuditElements.Result.FAILURE;
                output = e;

                LOG.warn("Error pushing {} towards {}", subject, profile.getTask().getResource(), e);
                throw new JobExecutionException(e);
            } finally {
                notificationManager.createTasks(
                        AuditElements.EventCategoryType.PUSH,
                        AttributableType.USER.name().toLowerCase(),
                        profile.getTask().getResource().getKey(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        subject);
                auditManager.audit(
                        AuditElements.EventCategoryType.PUSH,
                        AttributableType.USER.name().toLowerCase(),
                        profile.getTask().getResource().getKey(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        subject);
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

    protected Subject<?, ?, ?> update(final Subject<?, ?, ?> sbj, final Boolean enabled) {

        final Set<MembershipMod> membsToAdd = new HashSet<>();
        final Set<String> vattrToBeRemoved = new HashSet<>();
        final Set<String> membVattrToBeRemoved = new HashSet<>();
        final Set<AttrMod> vattrToBeUpdated = new HashSet<>();

        // Search for all mapped vattrs
        final Mapping<?> umapping = getMapping();
        for (MappingItem mappingItem : umapping.getItems()) {
            if (mappingItem.getIntMappingType() == IntMappingType.UserVirtualSchema) {
                vattrToBeRemoved.add(mappingItem.getIntAttrName());
            } else if (mappingItem.getIntMappingType() == IntMappingType.MembershipVirtualSchema) {
                membVattrToBeRemoved.add(mappingItem.getIntAttrName());
            }
        }

        // Search for all user's vattrs and:
        // 1. add mapped vattrs not owned by the user to the set of vattrs to be removed
        // 2. add all vattrs owned by the user to the set of vattrs to be update
        for (VirAttr vattr : sbj.getVirAttrs()) {
            vattrToBeRemoved.remove(vattr.getSchema().getKey());
            final AttrMod mod = new AttrMod();
            mod.setSchema(vattr.getSchema().getKey());
            mod.getValuesToBeAdded().addAll(vattr.getValues());
            vattrToBeUpdated.add(mod);
        }

        final boolean changepwd;

        if (sbj instanceof User) {
            changepwd = true;

            // Search for memberships
            for (Membership membership : User.class.cast(sbj).getMemberships()) {
                final MembershipMod membershipMod = new MembershipMod();
                membershipMod.setKey(membership.getKey());
                membershipMod.setRole(membership.getRole().getKey());

                for (VirAttr vattr : membership.getVirAttrs()) {
                    membVattrToBeRemoved.remove(vattr.getSchema().getKey());
                    final AttrMod mod = new AttrMod();
                    mod.setSchema(vattr.getSchema().getKey());
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

        final List<String> noPropResources = new ArrayList<>(sbj.getResourceNames());
        noPropResources.remove(profile.getTask().getResource().getKey());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUpdateTaskIds(
                sbj, null, changepwd, enabled, vattrToBeRemoved, vattrToBeUpdated, propByRes, noPropResources,
                membsToAdd));

        return userDAO.authFetch(sbj.getKey());
    }
}
