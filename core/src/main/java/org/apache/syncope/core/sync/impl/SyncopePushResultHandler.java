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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.propagation.impl.AbstractPropagationTaskExecutor;
import org.apache.syncope.core.sync.PushActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Transactional;

public class SyncopePushResultHandler extends AbstractSyncopeResultHandler<PushTask, PushActions> {

    protected Map<Long, String> roleOwnerMap = new HashMap<Long, String>();

    public Map<Long, String> getRoleOwnerMap() {
        return roleOwnerMap;
    }

    @Transactional
    public boolean handle(final AbstractAttributable attributable) {
        try {
            doHandle(attributable);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    /**
     * Look into SyncDelta and take necessary actions (create / update / delete) on user(s).
     *
     * @param delta returned by the underlying connector
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected final void doHandle(final AbstractAttributable attributable)
            throws JobExecutionException {

        if (results == null) {
            results = new ArrayList<SyncResult>();
        }

        final AttributableUtil attrUtil = AttributableUtil.getInstance(attributable);

        final SyncResult result = new SyncResult();
        results.add(result);

        result.setId(attributable.getId());
        result.setSubjectType(attrUtil.getType());

        final AbstractAttributable toBeHandled;
        final Boolean enabled;

        if (attrUtil.getType() == AttributableType.USER) {
            toBeHandled = userDataBinder.getUserFromId(attributable.getId());
            result.setName(((SyncopeUser) toBeHandled).getUsername());
            enabled = getSyncTask().isSyncStatus()
                    ? ((SyncopeUser) toBeHandled).isSuspended() ? Boolean.FALSE : Boolean.TRUE
                    : null;
        } else {
            toBeHandled = roleDataBinder.getRoleFromId(attributable.getId());
            result.setName(((SyncopeRole) toBeHandled).getName());
            enabled = null;
        }

        LOG.debug("Propagating {} with ID {} towards {}",
                attrUtil.getType(), toBeHandled.getId(), getSyncTask().getResource());

        Object output = null;
        Result resultStatus = null;
        ConnectorObject beforeObj = null;
        Map.Entry<String, Set<Attribute>> values = null;

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
                    getSyncTask().getResource()); // target external resource

            final ObjectClass oclass =
                    attrUtil.getType() == AttributableType.USER ? ObjectClass.ACCOUNT : ObjectClass.GROUP;

            // Try to read remote object (user / group) BEFORE any actual operation
            beforeObj = getRemoteObject(oclass, values.getKey(), getSyncTask().getResource().getName());

            if (beforeObj == null) {
                result.setOperation(ResourceOperation.CREATE);
                actions.beforeCreate(this, values, toBeHandled);
            } else {
                result.setOperation(ResourceOperation.UPDATE);
                actions.beforeUpdate(this, values, toBeHandled);
            }

            AbstractPropagationTaskExecutor.createOrUpdate(
                    oclass,
                    values.getKey(),
                    values.getValue(),
                    getSyncTask().getResource().getName(),
                    getSyncTask().getResource().getPropagationMode(),
                    beforeObj,
                    connector,
                    new HashSet<String>(),
                    connObjectUtil);

            result.setStatus(SyncResult.Status.SUCCESS);
            resultStatus = AuditElements.Result.SUCCESS;
        } catch (Exception e) {
            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage(e.getMessage());
            resultStatus = AuditElements.Result.FAILURE;
            output = e;

            LOG.warn("Error pushing {} towards {}", toBeHandled, getSyncTask().getResource(), e);
            throw new JobExecutionException(e);
        } finally {

            actions.after(this, values, toBeHandled, result);

            notificationManager.createTasks(
                    AuditElements.EventCategoryType.PUSH,
                    AttributableType.USER.name().toLowerCase(),
                    syncTask.getResource().getName(),
                    result.getOperation() == null ? null : result.getOperation().name().toLowerCase(),
                    resultStatus,
                    beforeObj,
                    output,
                    toBeHandled);

            auditManager.audit(
                    AuditElements.EventCategoryType.PUSH,
                    AttributableType.USER.name().toLowerCase(),
                    syncTask.getResource().getName(),
                    result.getOperation() == null ? null : result.getOperation().name().toLowerCase(),
                    resultStatus,
                    beforeObj,
                    output,
                    toBeHandled);
        }
    }

    private ConnectorObject getRemoteObject(
            final ObjectClass oclass, final String accountId, final String resource) {
        ConnectorObject obj = null;

        try {

            final Uid uid = new Uid(accountId);

            connector.getObject(
                    oclass, uid, connector.getOperationOptions(Collections.<AbstractMappingItem>emptySet()));

        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }
        return obj;
    }
}
