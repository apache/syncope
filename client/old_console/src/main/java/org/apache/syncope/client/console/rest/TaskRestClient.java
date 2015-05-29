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
package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.client.console.SyncopeSession;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Tasks services.
 */
@Component
public class TaskRestClient extends JobRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 6284485820911028843L;

    public List<String> getJobClasses() {
        return SyncopeSession.get().getSyncopeTO().getTaskJobs();
    }

    public List<String> getSyncActionsClasses() {
        return SyncopeSession.get().getSyncopeTO().getSyncActions();
    }

    public List<String> getPushActionsClasses() {
        return SyncopeSession.get().getSyncopeTO().getPushActions();
    }

    /**
     * Return the number of tasks.
     *
     * @param kind of task (propagation, sched, sync).
     * @return number of stored tasks.
     */
    public int count(final String kind) {
        return getService(TaskService.class).list(TaskType.fromString(kind), 1, 1).getTotalCount();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(final Class<T> reference,
            final int page, final int size, final SortParam<String> sort) {

        return (List<T>) getService(TaskService.class).list(getTaskType(reference), page, size, toOrderBy(sort)).
                getResult();
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
        } else if (PushTaskTO.class.equals(reference)) {
            result = TaskType.PUSH;
        }
        return result;
    }

    public PropagationTaskTO readPropagationTask(final Long taskId) {
        return getService(TaskService.class).read(taskId);
    }

    public NotificationTaskTO readNotificationTask(final Long taskId) {
        return getService(TaskService.class).read(taskId);
    }

    public <T extends SchedTaskTO> T readSchedTask(final Class<T> reference, final Long taskId) {
        return getService(TaskService.class).read(taskId);
    }

    public void delete(final Long taskId, final Class<? extends AbstractTaskTO> taskToClass) {
        getService(TaskService.class).delete(taskId);
    }

    @Override
    public void startExecution(final long taskId) {
        startExecution(taskId, false);
    }

    public void startExecution(final long taskId, final boolean dryRun) {
        getService(TaskService.class).execute(taskId, dryRun);
    }

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
        getService(TaskService.class).update(taskTO.getKey(), taskTO);
    }

    public void updateSyncTask(final SyncTaskTO taskTO) {
        getService(TaskService.class).update(taskTO.getKey(), taskTO);
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(TaskService.class).bulk(action);
    }

    @Override
    public boolean isJobRunning(final long taskId) {
        for (TaskExecTO taskExecTO : getService(TaskService.class).listJobs(JobStatusType.RUNNING)) {
            if (taskExecTO.getTask() == taskId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startJob(final long taskId) {
        getService(TaskService.class).actionJob(taskId, JobAction.START);
    }

    @Override
    public void stopJob(final long taskId) {
        getService(TaskService.class).actionJob(taskId, JobAction.STOP);
    }
}
