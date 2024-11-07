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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest Tasks services.
 */
public class TaskRestClient extends BaseRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 6284485820911028843L;

    public JobTO getJob(final String key) {
        return getService(TaskService.class).getJob(key);
    }

    public List<JobTO> listJobs() {
        return getService(TaskService.class).listJobs();
    }

    public void actionJob(final String refKey, final JobAction jobAction) {
        getService(TaskService.class).actionJob(refKey, jobAction);
    }

    public long count(final TaskType kind) {
        return getService(TaskService.class).search(
                new TaskQuery.Builder(kind).page(1).size(0).build()).getTotalCount();
    }

    public long count(final String resource, final TaskType kind) {
        return getService(TaskService.class).search(
                new TaskQuery.Builder(kind).resource(resource).page(1).size(0).build()).getTotalCount();
    }

    public long count(final AnyTypeKind anyTypeKind, final String entityKey, final TaskType kind) {
        return getService(TaskService.class).search(
                new TaskQuery.Builder(kind).anyTypeKind(anyTypeKind).entityKey(entityKey).page(1).size(0).build()).
                getTotalCount();
    }

    public long count(final AnyTypeKind anyTypeKind, final String entityKey, final String notification) {
        return getService(TaskService.class).search(
                new TaskQuery.Builder(TaskType.NOTIFICATION).notification(notification).
                        anyTypeKind(anyTypeKind).entityKey(entityKey).page(1).size(0).build()).
                getTotalCount();
    }

    @Override
    public long countExecutions(final String taskKey) {
        return getService(TaskService.class).
                listExecutions(new ExecQuery.Builder().key(taskKey).page(1).size(0).build()).getTotalCount();
    }

    public List<PropagationTaskTO> listPropagationTasks(
            final String resource, final int page, final int size, final SortParam<String> sort) {

        return getService(TaskService.class).
                <PropagationTaskTO>search(new TaskQuery.Builder(TaskType.PROPAGATION).
                        resource(resource).
                        page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public List<PropagationTaskTO> listPropagationTasks(
            final AnyTypeKind anyTypeKind, final String entityKey,
            final int page, final int size, final SortParam<String> sort) {

        return getService(TaskService.class).
                <PropagationTaskTO>search(new TaskQuery.Builder(TaskType.PROPAGATION).
                        anyTypeKind(anyTypeKind).entityKey(entityKey).
                        page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public List<NotificationTaskTO> listNotificationTasks(
            final String notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final int page,
            final int size,
            final SortParam<String> sort) {

        TaskQuery.Builder builder = new TaskQuery.Builder(TaskType.NOTIFICATION);
        if (notification != null) {
            builder.notification(notification);
        }

        if (anyTypeKind != null) {
            builder.anyTypeKind(anyTypeKind);
        }

        if (entityKey != null) {
            builder.entityKey(entityKey);
        }

        PagedResult<NotificationTaskTO> list = getService(TaskService.class).
                search(builder.page(page).size(size).orderBy(toOrderBy(sort)).build());
        return list.getResult();
    }

    public <T extends TaskTO> List<T> list(
            final TaskType taskType, final int page, final int size, final SortParam<String> sort) {

        return getService(TaskService.class).<T>search(
                new TaskQuery.Builder(taskType).
                        page(page).
                        size(size).
                        orderBy(toOrderBy(sort)).
                        build()).
                getResult();
    }

    public <T extends TaskTO> List<T> list(
            final String resource,
            final TaskType taskType,
            final int page,
            final int size,
            final SortParam<String> sort) {

        return getService(TaskService.class).<T>search(
                new TaskQuery.Builder(taskType).
                        page(page).
                        size(size).
                        resource(resource).
                        orderBy(toOrderBy(sort)).
                        build()).
                getResult();
    }

    @Override
    public List<ExecTO> listExecutions(
            final String taskKey, final int page, final int size, final SortParam<String> sort) {

        return getService(TaskService.class).
                listExecutions(new ExecQuery.Builder().key(taskKey).page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).getResult();
    }

    public PropagationTaskTO readPropagationTask(final String taskKey) {
        return getService(TaskService.class).read(TaskType.PROPAGATION, taskKey, false);
    }

    public NotificationTaskTO readNotificationTask(final String taskKey) {
        return getService(TaskService.class).read(TaskType.NOTIFICATION, taskKey, false);
    }

    public <T extends TaskTO> T readTask(final TaskType type, final String taskKey) {
        return getService(TaskService.class).read(type, taskKey, false);
    }

    public SyncopeForm getMacroTaskForm(final String taskKey) {
        return getService(TaskService.class).
                getMacroTaskForm(taskKey, SyncopeConsoleSession.get().getLocale().toLanguageTag());
    }

    public void delete(final TaskType type, final String taskKey) {
        getService(TaskService.class).delete(type, taskKey);
    }

    @Override
    public void startExecution(final String taskKey, final Date startAt) {
        startExecution(taskKey, startAt, false);
    }

    public void startExecution(final String taskKey, final Date startAt, final boolean dryRun) {
        getService(TaskService.class).execute(
                new ExecSpecs.Builder().key(taskKey).startAt(DateOps.toOffsetDateTime(startAt)).dryRun(dryRun).build());
    }

    public void startExecution(
            final String taskKey,
            final Date startAt,
            final boolean dryRun,
            final SyncopeForm macroTaskForm) {

        getService(TaskService.class).execute(
                new ExecSpecs.Builder().key(taskKey).startAt(DateOps.toOffsetDateTime(startAt)).dryRun(dryRun).build(),
                macroTaskForm);
    }

    @Override
    public void deleteExecution(final String taskExecKey) {
        getService(TaskService.class).deleteExecution(taskExecKey);
    }

    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return getService(TaskService.class).listRecentExecutions(max);
    }

    public void create(final TaskType type, final SchedTaskTO taskTO) {
        getService(TaskService.class).create(type, taskTO);
    }

    public void update(final TaskType type, final SchedTaskTO taskTO) {
        getService(TaskService.class).update(type, taskTO);
    }

    @Override
    public Map<String, String> batch(final BatchRequest batchRequest) {
        List<BatchRequestItem> batchRequestItems = new ArrayList<>(batchRequest.getItems());

        Map<String, String> result = new LinkedHashMap<>();
        try {
            List<BatchResponseItem> batchResponseItems = batchRequest.commit().getItems();
            for (int i = 0; i < batchResponseItems.size(); i++) {
                String status = getStatus(batchResponseItems.get(i).getStatus());

                if (batchRequestItems.get(i).getRequestURI().contains("/execute")) {
                    result.put(StringUtils.substringAfterLast(
                            StringUtils.substringBefore(batchRequestItems.get(i).getRequestURI(), "/execute"), "/"),
                            status);
                } else {
                    result.put(StringUtils.substringAfterLast(
                            batchRequestItems.get(i).getRequestURI(), "/"), status);
                }
            }
        } catch (IOException e) {
            LOG.error("While processing Batch response", e);
        }

        return result;
    }
}
