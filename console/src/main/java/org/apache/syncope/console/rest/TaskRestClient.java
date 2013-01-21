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
import java.util.Collections;
import java.util.List;

import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.types.TaskType;
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
        List<String> jobClasses = null;

        try {
            jobClasses = new ArrayList<String>(getService(TaskService.class).getJobClasses());
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all job classes", e);
        }
        return jobClasses;
    }

    public List<String> getSyncActionsClasses() {
        List<String> actions = null;

        try {
            actions = new ArrayList<String>(getService(TaskService.class).getSyncActionsClasses());
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all sync actions classes", e);
        }
        return actions;
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
    public <T extends TaskTO> List<T> listTasks(final Class<T> reference, final int page, final int size) {
        List<T> result = Collections.emptyList();
        result = getService(TaskService.class).list(getTaskType(reference), page, size);
        return result;
    }

    private TaskType getTaskType(Class<?> reference) {
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
     * Get all executions.
     *
     * @return list of all executions
     */
    @Override
    public List<TaskExecTO> listExecutions() {
        throw new UnsupportedOperationException("You need to specify type of executed tasks to be listed");
//        return getService(TaskService.class).listExecutions();
//                Arrays.asList(SyncopeSession.get().getRestTemplate()
//                .getForObject(baseURL + "task/execution/list", TaskExecTO[].class)); FIXME interface?
    }

    /**
     * Delete specified task.
     *
     * @param taskId task to delete
     */
    public TaskTO delete(final Long taskId, final Class<? extends TaskTO> taskToClass) {
        return getService(TaskService.class).delete(getTaskType(taskToClass), taskId);
    }

    @Override
    public void startExecution(final Long taskId) {
        startExecution(taskId, false);
    }

    /**
     * Start execution for the specified TaskTO.
     *
     * @param taskId task id
     */
    public void startExecution(final Long taskId, boolean dryRun) {
        getService(TaskService.class).execute(taskId, dryRun);
    }

    /**
     * Delete specified task's execution.
     *
     * @param taskExecId task execution id
     */
    @Override
    public void deleteExecution(final Long taskExecId) {
        getService(TaskService.class).deleteExecution(taskExecId);
    }

    public SyncTaskTO createSyncTask(final SyncTaskTO taskTO) {
        return getService(TaskService.class).create(taskTO);
    }

    public SchedTaskTO createSchedTask(final SchedTaskTO taskTO) {
        return getService(TaskService.class).create(taskTO);
    }

    public SchedTaskTO updateSchedTask(final SchedTaskTO taskTO) {
        return getService(TaskService.class).update(taskTO.getId(), taskTO);
    }

    public SyncTaskTO updateSyncTask(final SyncTaskTO taskTO) {
        return getService(TaskService.class).update(taskTO.getId(), taskTO);
    }
}
