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

import java.util.Date;
import java.util.List;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskExecQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest Tasks services.
 */
public class TaskRestClient extends BaseRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 6284485820911028843L;

    public int count(final TaskType kind) {
        return getService(TaskService.class).list(
                new TaskQuery.Builder().type(kind).page(1).size(1).build()).getTotalCount();
    }

    public int count(final String resource, final TaskType kind) {
        return getService(TaskService.class).list(
                new TaskQuery.Builder().resource(resource).type(kind).page(1).size(1).
                build()).getTotalCount();
    }

    public int count(final AnyTypeKind anyTypeKind, final Long anyTypeKey, final TaskType kind) {
        return getService(TaskService.class).list(
                new TaskQuery.Builder().anyTypeKind(anyTypeKind).anyTypeKey(anyTypeKey).type(kind).page(1).size(1).
                build()).getTotalCount();
    }

    public int countExecutions(final Long taskKey) {
        return getService(TaskService.class).
                listExecutions(new TaskExecQuery.Builder().key(taskKey).page(1).size(1).build()).getTotalCount();
    }

    public List<PropagationTaskTO> listPropagationTasks(
            final String resource, final int page, final int size, final SortParam<String> sort) {

        return getService(TaskService.class).
                <PropagationTaskTO>list(new TaskQuery.Builder().type(TaskType.PROPAGATION).
                        resource(resource).
                        page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public List<PropagationTaskTO> listPropagationTasks(
            final AnyTypeKind anyTypeKind, final Long anyTypeKey,
            final int page, final int size, final SortParam<String> sort) {

        return getService(TaskService.class).
                <PropagationTaskTO>list(new TaskQuery.Builder().type(TaskType.PROPAGATION).
                        anyTypeKind(anyTypeKind).anyTypeKey(anyTypeKey).
                        page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(
            final Class<T> reference, final int page, final int size, final SortParam<String> sort) {

        return (List<T>) getService(TaskService.class).
                list(new TaskQuery.Builder().type(getTaskType(reference)).page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(
            final String resource,
            final Class<T> reference,
            final int page,
            final int size,
            final SortParam<String> sort) {

        return (List<T>) getService(TaskService.class).
                list(new TaskQuery.Builder().type(getTaskType(reference)).page(page).size(size).resource(resource).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public List<ExecTO> listExecutions(final Long taskId, final int page, final int size) {
        return getService(TaskService.class).
                listExecutions(new TaskExecQuery.Builder().key(taskId).page(page).size(size).build()).getResult();
    }

    private TaskType getTaskType(final Class<?> reference) {
        TaskType result = null;
        if (PropagationTaskTO.class.equals(reference)) {
            result = TaskType.PROPAGATION;
        } else if (NotificationTaskTO.class.equals(reference)) {
            result = TaskType.NOTIFICATION;
        } else if (SchedTaskTO.class.equals(reference)) {
            result = TaskType.SCHEDULED;
        } else if (PullTaskTO.class.equals(reference)) {
            result = TaskType.PULL;
        } else if (PushTaskTO.class.equals(reference)) {
            result = TaskType.PUSH;
        }
        return result;
    }

    public PropagationTaskTO readPropagationTask(final Long taskKey) {
        return getService(TaskService.class).read(taskKey, false);
    }

    public NotificationTaskTO readNotificationTask(final Long taskKey) {
        return getService(TaskService.class).read(taskKey, false);
    }

    public <T extends SchedTaskTO> T readSchedTask(final Class<T> reference, final Long taskKey) {
        return getService(TaskService.class).read(taskKey, false);
    }

    public void delete(final Long taskKey, final Class<? extends AbstractTaskTO> taskToClass) {
        getService(TaskService.class).delete(taskKey);
    }

    @Override
    public void startExecution(final long taskKey, final Date start) {
        startExecution(taskKey, start, false);
    }

    public void startExecution(final long taskKey, final Date start, final boolean dryRun) {
        getService(TaskService.class).execute(
                new ExecuteQuery.Builder().key(taskKey).startAt(start).dryRun(dryRun).build());
    }

    @Override
    public void deleteExecution(final long taskExecId) {
        getService(TaskService.class).deleteExecution(taskExecId);
    }

    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return getService(TaskService.class).listRecentExecutions(max);
    }

    public void create(final SchedTaskTO taskTO) {
        getService(TaskService.class).create(taskTO);
    }

    public void update(final SchedTaskTO taskTO) {
        getService(TaskService.class).update(taskTO);
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(TaskService.class).bulk(action);
    }
}
