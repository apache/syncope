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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class LiveSyncTaskSaver {

    protected final ExternalResourceDAO resourceDAO;

    protected final TaskDAO taskDAO;

    protected final TaskExecDAO taskExecDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final NotificationManager notificationManager;

    protected final AuditManager auditManager;

    public LiveSyncTaskSaver(
            final ExternalResourceDAO resourceDAO,
            final TaskDAO taskDAO,
            final TaskExecDAO taskExecDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final NotificationManager notificationManager,
            final AuditManager auditManager) {

        this.resourceDAO = resourceDAO;
        this.taskDAO = taskDAO;
        this.taskExecDAO = taskExecDAO;
        this.taskUtilsFactory = taskUtilsFactory;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addAttr(
            final AnyUtils anyUtils,
            final PlainAttrValidationManager validator,
            final String key,
            final PlainSchema schema,
            final String value) {

        anyUtils.addAttr(validator, key, schema, value);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            final String taskKey,
            final TaskExec<SchedTask> execution,
            final String message,
            final String status,
            final OpEvent.Outcome result,
            final Function<TaskExec<SchedTask>, Boolean> hasToBeRegistered) throws JobExecutionException {

        LiveSyncTask task = (LiveSyncTask) taskDAO.findById(TaskType.LIVE_SYNC, taskKey).
                orElseThrow(() -> new JobExecutionException("Not found: " + TaskType.LIVE_SYNC + " Task " + taskKey));

        execution.setTask(task);
        execution.setMessage(message);
        execution.setStatus(status);
        execution.setEnd(OffsetDateTime.now());

        if (hasToBeRegistered.apply(execution)) {
            taskExecDAO.saveAndAdd(TaskType.LIVE_SYNC, task.getKey(), execution);
        }
        task = taskDAO.save(task);

        notificationManager.createTasks(
                execution.getExecutor(),
                OpEvent.CategoryType.TASK,
                this.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(),
                result,
                task,
                execution);

        auditManager.audit(
                AuthContextUtils.getDomain(),
                execution.getExecutor(),
                OpEvent.CategoryType.TASK,
                task.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(),
                result,
                task,
                execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            final String resourceKey,
            final Map<String, SyncToken> latestSyncTokens) throws JobExecutionException {

        ExternalResource resource = resourceDAO.findById(resourceKey).
                orElseThrow(() -> new JobExecutionException("Not found: ExternalResource " + resourceKey));

        latestSyncTokens.forEach((objectClass, syncToken) -> {
            resource.getProvisionByObjectClass(objectClass).
                    ifPresent(p -> p.setSyncToken(ConnObjectUtils.toString(syncToken)));
            Optional.ofNullable(resource.getOrgUnit()).
                    filter(ou -> objectClass.equals(ou.getObjectClass())).
                    ifPresent(ou -> ou.setSyncToken(ConnObjectUtils.toString(syncToken)));
        });

        resourceDAO.save(resource);
    }
}
