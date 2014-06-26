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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.sync.PushActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractSubjectPushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions> {

    protected abstract String getName(final AbstractSubject subject);

    protected abstract AbstractSubjectTO getSubjectTO(final long id);

    protected abstract AbstractSubject getSubject(final long id);

    protected abstract AbstractSubject deprovision(final AbstractSubject sbj, final SyncResult result);

    protected abstract AbstractSubject provision(
            final AbstractSubject sbj, final Boolean enabled, final SyncResult result);

    protected abstract AbstractSubject link(final AbstractSubject sbj, final Boolean unlink, final SyncResult result);

    protected abstract AbstractSubject unassign(final AbstractSubject sbj, final SyncResult result);

    protected abstract AbstractSubject assign(final AbstractSubject sbj, Boolean enabled, final SyncResult result);

    protected abstract AbstractSubject update(
            final AbstractSubject sbj,
            final String accountId,
            final Set<Attribute> attributes,
            final ConnectorObject beforeObj,
            final SyncResult result);

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
        Map.Entry<String, Set<Attribute>> values = null;

        String operation = null;

        try {
            values = MappingUtil.prepareAttributes(
                    attrUtil, // attributable util
                    toBeHandled, // attributable (user or role)
                    null, // current password if decode is possible; generate otherwise
                    true, // propagate password (if required)
                    null, // no vir attrs to be removed
                    null, // propagate current vir attr values
                    null, // no membership vir attrs to be removed
                    null, // propagate current membership vir attr values
                    enabled, // propagate status (suspended or not) if required
                    profile.getSyncTask().getResource()); // target external resource

            // Try to read remote object (user / group) BEFORE any actual operation
            beforeObj = getRemoteObject(values.getKey());

            if (beforeObj == null) {
                operation = profile.getSyncTask().getUnmatchigRule().name().toLowerCase();
                switch (profile.getSyncTask().getUnmatchigRule()) {
                    case ASSIGN:
                        result.setOperation(ResourceOperation.CREATE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeAssign(this.getProfile(), values, toBeHandled);
                        }
                        assign(toBeHandled, enabled, result);
                        break;
                    case PROVISION:
                        result.setOperation(ResourceOperation.CREATE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeProvision(this.getProfile(), values, toBeHandled);
                        }
                        provision(toBeHandled, enabled, result);
                        break;
                    case UNLINK:
                        result.setOperation(ResourceOperation.NONE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeUnlink(this.getProfile(), values, toBeHandled);
                        }
                        link(toBeHandled, false, result);
                        break;
                    default:
                    // do nothing
                }

            } else {
                operation = profile.getSyncTask().getMatchigRule().name().toLowerCase();
                switch (profile.getSyncTask().getMatchigRule()) {
                    case UPDATE:
                        result.setOperation(ResourceOperation.UPDATE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeUpdate(this.getProfile(), values, toBeHandled);
                        }
                        update(toBeHandled, values.getKey(), values.getValue(), beforeObj, result);
                        break;
                    case DEPROVISION:
                        result.setOperation(ResourceOperation.DELETE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeDeprovision(this.getProfile(), values, toBeHandled);
                        }
                        deprovision(toBeHandled, result);
                        break;
                    case UNASSIGN:
                        result.setOperation(ResourceOperation.DELETE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeUnassign(this.getProfile(), values, toBeHandled);
                        }
                        unassign(toBeHandled, result);
                        break;
                    case LINK:
                        result.setOperation(ResourceOperation.NONE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeLink(this.getProfile(), values, toBeHandled);
                        }
                        link(toBeHandled, true, result);
                        break;
                    case UNLINK:
                        result.setOperation(ResourceOperation.NONE);
                        for (PushActions action : profile.getActions()) {
                            action.beforeUnlink(this.getProfile(), values, toBeHandled);
                        }
                        link(toBeHandled, false, result);
                        break;
                    default:
                    // do nothing
                }
            }

            result.setStatus(SyncResult.Status.SUCCESS);
            resultStatus = AuditElements.Result.SUCCESS;
            output = getRemoteObject(values.getKey());
        } catch (Exception e) {
            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            resultStatus = AuditElements.Result.FAILURE;
            output = e;

            LOG.warn("Error pushing {} towards {}", toBeHandled, profile.getSyncTask().getResource(), e);
            throw new JobExecutionException(e);
        } finally {
            for (PushActions action : profile.getActions()) {
                action.after(this.getProfile(), values, toBeHandled, result);
            }
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
