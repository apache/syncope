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
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.propagation.impl.AbstractPropagationTaskExecutor;
import org.apache.syncope.core.rest.controller.AbstractSubjectController;
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

        if (results == null) {
            results = new ArrayList<SyncResult>();
        }

        final AttributableUtil attrUtil = AttributableUtil.getInstance(subject);

        final SyncResult result = new SyncResult();
        results.add(result);

        result.setId(subject.getId());
        result.setSubjectType(attrUtil.getType());

        final AbstractSubjectController<?, ?> controller;
        final AbstractSubject toBeHandled;
        final Boolean enabled;

        if (attrUtil.getType() == AttributableType.USER) {
            toBeHandled = userDataBinder.getUserFromId(subject.getId());
            result.setName(((SyncopeUser) toBeHandled).getUsername());
            enabled = getSyncTask().isSyncStatus()
                    ? ((SyncopeUser) toBeHandled).isSuspended() ? Boolean.FALSE : Boolean.TRUE
                    : null;
            controller = userController;
        } else {
            toBeHandled = roleDataBinder.getRoleFromId(subject.getId());
            result.setName(((SyncopeRole) toBeHandled).getName());
            enabled = null;
            controller = roleController;
        }

        LOG.debug("Propagating {} with ID {} towards {}",
                attrUtil.getType(), toBeHandled.getId(), getSyncTask().getResource());

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
                    getSyncTask().getResource()); // target external resource

            final ObjectClass oclass =
                    attrUtil.getType() == AttributableType.USER ? ObjectClass.ACCOUNT : ObjectClass.GROUP;

            // Try to read remote object (user / group) BEFORE any actual operation
            beforeObj = getRemoteObject(oclass, values.getKey(), getSyncTask().getResource().getName());

            if (beforeObj == null) {
                operation = getSyncTask().getUnmatchigRule().name().toLowerCase();
                switch (getSyncTask().getUnmatchigRule()) {
                    case ASSIGN:
                        result.setOperation(ResourceOperation.CREATE);
                        for (PushActions action : actions) {
                            action.beforeAssign(this, values, toBeHandled);
                        }
                        controller.assign(
                                toBeHandled.getId(),
                                Collections.singleton(getSyncTask().getResource().getName()), true, null);
                        break;
                    case PROVISION:
                        result.setOperation(ResourceOperation.CREATE);
                        for (PushActions action : actions) {
                            action.beforeProvision(this, values, toBeHandled);
                        }
                        controller.provision(
                                toBeHandled.getId(),
                                Collections.singleton(getSyncTask().getResource().getName()), true, null);
                        break;
                    case UNLINK:
                        result.setOperation(ResourceOperation.NONE);
                        for (PushActions action : actions) {
                            action.beforeUnlink(this, values, toBeHandled);
                        }
                        controller.unlink(
                                toBeHandled.getId(), Collections.singleton(getSyncTask().getResource().getName()));
                        break;
                    default:
                    // do nothing
                }

            } else {
                operation = getSyncTask().getMatchigRule().name().toLowerCase();
                switch (getSyncTask().getMatchigRule()) {
                    case UPDATE:
                        result.setOperation(ResourceOperation.UPDATE);
                        for (PushActions action : actions) {
                            action.beforeUpdate(this, values, toBeHandled);
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
                        break;
                    case DEPROVISION:
                        result.setOperation(ResourceOperation.DELETE);
                        for (PushActions action : actions) {
                            action.beforeDeprovision(this, values, toBeHandled);
                        }
                        controller.deprovision(
                                toBeHandled.getId(), Collections.singleton(getSyncTask().getResource().getName()));
                        break;
                    case UNASSIGN:
                        result.setOperation(ResourceOperation.DELETE);
                        for (PushActions action : actions) {
                            action.beforeUnassign(this, values, toBeHandled);
                        }
                        controller.unlink(
                                toBeHandled.getId(), Collections.singleton(getSyncTask().getResource().getName()));
                        controller.deprovision(
                                toBeHandled.getId(), Collections.singleton(getSyncTask().getResource().getName()));
                        break;
                    case LINK:
                        result.setOperation(ResourceOperation.NONE);
                        for (PushActions action : actions) {
                            action.beforeLink(this, values, toBeHandled);
                        }
                        controller.link(
                                toBeHandled.getId(), Collections.singleton(getSyncTask().getResource().getName()));
                        break;
                    case UNLINK:
                        result.setOperation(ResourceOperation.NONE);
                        for (PushActions action : actions) {
                            action.beforeUnlink(this, values, toBeHandled);
                        }
                        controller.unlink(
                                toBeHandled.getId(), Collections.singleton(getSyncTask().getResource().getName()));
                        break;
                    default:
                    // do nothing
                }
            }

            result.setStatus(SyncResult.Status.SUCCESS);
            resultStatus = AuditElements.Result.SUCCESS;
            output = getRemoteObject(oclass, values.getKey(), getSyncTask().getResource().getName());
        } catch (Exception e) {
            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage(e.getMessage());
            resultStatus = AuditElements.Result.FAILURE;
            output = e;

            LOG.warn("Error pushing {} towards {}", toBeHandled, getSyncTask().getResource(), e);
            throw new JobExecutionException(e);
        } finally {
            for (PushActions action : actions) {
                action.after(this, values, toBeHandled, result);
            }
            notificationManager.createTasks(
                    AuditElements.EventCategoryType.PUSH,
                    AttributableType.USER.name().toLowerCase(),
                    syncTask.getResource().getName(),
                    operation,
                    resultStatus,
                    beforeObj,
                    output,
                    toBeHandled);
            auditManager.audit(
                    AuditElements.EventCategoryType.PUSH,
                    AttributableType.USER.name().toLowerCase(),
                    syncTask.getResource().getName(),
                    operation,
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
