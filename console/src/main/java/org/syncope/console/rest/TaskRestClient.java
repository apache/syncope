/* 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Tasks services.
 */
@Component
public class TaskRestClient extends AbstractBaseRestClient {

    /**
     * Return a list of job classes.
     * @return list of classes.
     */
    public Set<String> getJobClasses() {
        Set<String> validators = null;

        try {

            validators = restTemplate.getForObject(
                    baseURL + "task/jobClasses.json", Set.class);

        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all job classes", e);
        }
        return validators;
    }

    /**
     * Return the number of tasks.
     * @param kind of task (propagation, sched, sync).
     * @return number of stored tasks.
     */
    public Integer count(final String kind) {
        return restTemplate.getForObject(baseURL + "task/{kind}/count.json",
                Integer.class, kind);
    }

    /**
     * Return a paginated list of generic tasks.
     * @param page number.
     * @param size per page.
     * @return paginated list.
     */
    public List<SchedTaskTO> listSchedTasks(
            final int page, final int size) {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "task/sched/list/{page}/{size}.json",
                SchedTaskTO[].class, page, size));
    }

    /**
     * Return a paginated list of sync tasks.
     * @param page number.
     * @param size per page.
     * @return paginated list.
     */
    public List<SyncTaskTO> listSyncTasks(
            final int page, final int size) {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "task/sync/list/{page}/{size}.json",
                SyncTaskTO[].class, page, size));
    }

    /**
     * Return a paginated list of propagation tasks.
     * @param page number.
     * @param size per page.
     * @return paginated list.
     */
    public List<PropagationTaskTO> listPropagationTasks(
            final int page, final int size) {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "task/propagation/list/{page}/{size}.json",
                PropagationTaskTO[].class, page, size));
    }

    /**
     * Load an existing task.
     * @param taskId task to read.
     * @return TaskTO object if the configuration exists, null otherwise.
     */
    public PropagationTaskTO read(final String taskId) {
        PropagationTaskTO taskTO = null;
        try {
            taskTO = restTemplate.getForObject(
                    baseURL + "task/read/{taskId}",
                    PropagationTaskTO.class, taskId);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a task", e);
        }

        return taskTO;
    }

    /**
     * Get all executions.
     * @return list of all executions
     */
    public List<TaskExecTO> listExecutions() {
        return Arrays.asList(
                restTemplate.getForObject(
                baseURL + "task/execution/list",
                TaskExecTO[].class));
    }

    /**
     * Delete specified task.
     * @param taskId task to delete
     */
    public void delete(final Long taskId) {
        restTemplate.delete(
                baseURL + "task/delete/{taskId}", taskId);
    }

    /**
     * Start execution for the specified TaskTO.
     * @param taskId task id
     */
    public void startExecution(final Long taskId) {
        restTemplate.getForObject(
                baseURL + "task/execute/{taskId}",
                TaskExecTO.class, taskId);
    }

    /**
     * Delete specified task's execution.
     * @param taskExecId task execution id
     */
    public void deleteExecution(final Long taskExecId) {
        restTemplate.delete(baseURL
                + "task/execution/delete/{execId}", taskExecId);
    }
}
