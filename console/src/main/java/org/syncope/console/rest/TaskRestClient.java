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
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.syncope.client.to.NotificationTaskTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Tasks services.
 */
@Component
public class TaskRestClient extends AbstractBaseRestClient
        implements ExecutionRestClient {

    /**
     * Return a list of job classes.
     *
     * @return list of classes.
     */
    public Set<String> getJobClasses() {
        Set<String> jobClasses = null;

        try {
            jobClasses = restTemplate.getForObject(
                    baseURL + "task/jobClasses.json", Set.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all job classes", e);
        }
        return jobClasses;
    }

    public Set<String> getJobActionsClasses() {
        Set<String> actions = null;

        try {
            actions = restTemplate.getForObject(
                    baseURL + "task/jobActionsClasses.json", Set.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all job actions classes", e);
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
        return restTemplate.getForObject(baseURL + "task/{kind}/count.json",
                Integer.class, kind);
    }

    /**
     * Return a paginated list of tasks.
     *
     * @param page number.
     * @param size per page.
     * @return paginated list.
     */
    public <T extends TaskTO> List<T> listTasks(
            final Class<T> reference, final int page, final int size) {

        List<T> result = Collections.EMPTY_LIST;

        if (PropagationTaskTO.class == reference) {
            result = (List<T>) Arrays.asList(restTemplate.getForObject(
                    baseURL + "task/propagation/list/{page}/{size}.json",
                    PropagationTaskTO[].class, page, size));
        } else if (NotificationTaskTO.class == reference) {
            result = (List<T>) Arrays.asList(restTemplate.getForObject(
                    baseURL + "task/notification/list/{page}/{size}.json",
                    NotificationTaskTO[].class, page, size));
        } else if (SchedTaskTO.class == reference) {
            result = (List<T>) Arrays.asList(restTemplate.getForObject(
                    baseURL + "task/sched/list/{page}/{size}.json",
                    SchedTaskTO[].class, page, size));
        } else if (SyncTaskTO.class == reference) {
            result = (List<T>) Arrays.asList(restTemplate.getForObject(
                    baseURL + "task/sync/list/{page}/{size}.json",
                    SyncTaskTO[].class, page, size));
        }

        return result;
    }

    public PropagationTaskTO readPropagationTask(final Long taskId) {
        return restTemplate.getForObject(
                baseURL + "task/read/{taskId}",
                PropagationTaskTO.class, taskId);
    }

    public NotificationTaskTO readNotificationTask(final Long taskId) {
        return restTemplate.getForObject(
                baseURL + "task/read/{taskId}",
                NotificationTaskTO.class, taskId);
    }

    public <T extends SchedTaskTO> T readSchedTask(
            final Class<T> reference, final Long taskId) {

        if (SyncTaskTO.class.getName().equals(reference.getName())) {
            return (T) restTemplate.getForObject(
                    baseURL + "task/read/{taskId}",
                    SyncTaskTO.class, taskId);
        } else {
            return (T) restTemplate.getForObject(
                    baseURL + "task/read/{taskId}",
                    SchedTaskTO.class, taskId);
        }
    }

    /**
     * Get all executions.
     *
     * @return list of all executions
     */
    @Override
    public List<TaskExecTO> listExecutions() {
        return Arrays.asList(
                restTemplate.getForObject(
                baseURL + "task/execution/list",
                TaskExecTO[].class));
    }

    /**
     * Delete specified task.
     *
     * @param taskId task to delete
     */
    public void delete(final Long taskId) {
        restTemplate.delete(
                baseURL + "task/delete/{taskId}", taskId);
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
        restTemplate.postForObject(
                baseURL + "task/execute/{taskId}?dryRun={dryRun}",
                null, TaskExecTO.class, taskId, dryRun);
    }

    /**
     * Delete specified task's execution.
     *
     * @param taskExecId task execution id
     */
    @Override
    public void deleteExecution(final Long taskExecId) {
        restTemplate.delete(baseURL
                + "task/execution/delete/{execId}", taskExecId);
    }

    public SyncTaskTO createSyncTask(final SyncTaskTO taskTO) {
        return restTemplate.postForObject(baseURL
                + "task/create/sync", taskTO, SyncTaskTO.class);
    }

    public SchedTaskTO createSchedTask(final SchedTaskTO taskTO) {
        return restTemplate.postForObject(baseURL
                + "task/create/sched", taskTO, SchedTaskTO.class);
    }

    public SchedTaskTO updateSchedTask(final SchedTaskTO taskTO) {
        return restTemplate.postForObject(baseURL
                + "task/update/sched", taskTO, SchedTaskTO.class);
    }

    public SyncTaskTO updateSyncTask(final SyncTaskTO taskTO) {
        return restTemplate.postForObject(baseURL
                + "task/update/sync", taskTO, SyncTaskTO.class);
    }
}
