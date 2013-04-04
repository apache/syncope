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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Tasks services.
 */
@Component
public class TaskRestClient extends BaseRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 6284485820911028843L;

    /**
     * Return a list of job classes.
     *
     * @return list of classes.
     */
    public List<String> getJobClasses() {
        List<JobClassTO> jobClasses = null;

        try {
            jobClasses = new ArrayList<JobClassTO>(getService(TaskService.class).getJobClasses());
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all job classes", e);
        }
        return CollectionWrapper.unwrapJobClasses(jobClasses);
    }

    public List<String> getSyncActionsClasses() {
        List<SyncActionClassTO> actions = null;

        try {
            actions = new ArrayList<SyncActionClassTO>(getService(TaskService.class).getSyncActionsClasses());
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all sync actions classes", e);
        }
        return CollectionWrapper.unwrapSyncActionClasses(actions);
    }

    /**
     * Return the number of tasks.
     *
     * @param kind of task (propagation, sched, sync).
     * @return number of stored tasks.
     */
    public Integer count(final String kind) {
        return getService(TaskService.class).count(TaskType.fromString(kind));
    }

    /**
     * Return a paginated list of tasks.
     *
     * @param page number.
     * @param size per page.
     * @return paginated list.
     */
    @SuppressWarnings("unchecked")
    public <T extends TaskTO> List<T> listTasks(final Class<T> reference, final int page, final int size) {
        return (List<T>) getService(TaskService.class).list(getTaskType(reference), page, size);
    }

    private TaskType getTaskType(final Class<?> reference) {
        TaskType result = null;
        if (PropagationTaskTO.class.equals(reference)) {
            result = TaskType.PROPAGATION;
        } else if (NotificationTaskTO.class.equals(reference)) {
            result = TaskType.NOTIFICATION;
        } else if (SchedTaskTO.class.equals(reference)) {
            result = TaskType.SCHEDULED;
        } else if (SyncTaskTO.class.equals(reference)) {
            result = TaskType.SYNCHRONIZATION;
        }
        return result;
    }

    public PropagationTaskTO readPropagationTask(final Long taskId) {
        return getService(TaskService.class).read(TaskType.PROPAGATION, taskId);
    }

    public NotificationTaskTO readNotificationTask(final Long taskId) {
        return getService(TaskService.class).read(TaskType.NOTIFICATION, taskId);
    }

    public <T extends SchedTaskTO> T readSchedTask(final Class<T> reference, final Long taskId) {
        return getService(TaskService.class).read(getTaskType(reference), taskId);
    }

    /**
     * Delete specified task.
     *
     * @param taskId task to delete
     */
    public void delete(final Long taskId, final Class<? extends TaskTO> taskToClass) {
        getService(TaskService.class).delete(taskId);
    }

    @Override
    public void startExecution(final long taskId) {
        startExecution(taskId, false);
    }

    /**
     * Start execution for the specified TaskTO.
     *
     * @param taskId task id
     */
    public void startExecution(final long taskId, final boolean dryRun) {
        getService(TaskService.class).execute(taskId, dryRun);
    }

    /**
     * Delete specified task's execution.
     *
     * @param taskExecId task execution id
     */
    @Override
    public void deleteExecution(final long taskExecId) {
        getService(TaskService.class).deleteExecution(taskExecId);
    }

    public void createSyncTask(final SyncTaskTO taskTO) {
        getService(TaskService.class).create(taskTO);
    }

    public void createSchedTask(final SchedTaskTO taskTO) {
        getService(TaskService.class).create(taskTO);
    }

    public void updateSchedTask(final SchedTaskTO taskTO) {
        getService(TaskService.class).update(taskTO.getId(), taskTO);
    }

    public void updateSyncTask(final SyncTaskTO taskTO) {
        getService(TaskService.class).update(taskTO.getId(), taskTO);
    }

    public BulkActionRes bulkAction(final BulkAction action) {
        return getService(TaskService.class).bulkAction(action);
    }
}
