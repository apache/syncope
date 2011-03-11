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
import org.springframework.stereotype.Component;
import org.syncope.client.to.TaskExecutionTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Tasks services.
 */
@Component
public class TaskRestClient extends AbstractBaseRestClient {

    public Integer count() {
        return restTemplate.getForObject(baseURL + "task/count.json",
                Integer.class);
    }

    /**
     * Get all stored tasks.
     * @param first index to start from
     * @param count maximum number to fetch
     * @return list of TaskTO objects
     */
    public List<TaskTO> list(final int first, final int count) {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "task/list/{page}/{size}.json",
                TaskTO[].class, first, count));
    }

    /**
     * Load an existing task.
     * @param taskId task to read
     * @return TaskTO object if the configuration exists, null otherwise
     */
    public TaskTO readTask(final String taskId) {
        TaskTO taskTO = null;
        try {
            taskTO = restTemplate.getForObject(
                    baseURL + "task/read/{taskId}",
                    TaskTO.class, taskId);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a task", e);
        }

        return taskTO;
    }

    /**
     * Get all executions.
     * @return list of all executions
     */
    public List<TaskExecutionTO> listExecutions() {
        return Arrays.asList(
                restTemplate.getForObject(
                baseURL + "task/execution/list",
                TaskExecutionTO[].class));
    }

    /**
     * Delete specified task.
     * @param taskId task to delete
     */
    public void deleteTask(final Long taskId) {
        restTemplate.delete(
                baseURL + "task/delete/{taskId}", taskId);
    }

    /**
     * Start execution for the specified TaskTO.
     * @param taskId task id
     */
    public void startTaskExecution(final Long taskId) {
        restTemplate.getForObject(
                baseURL + "task/execute/{taskId}",
                TaskExecutionTO.class, taskId);
    }

    /**
     * Delete specified task's execution.
     * @param taskExecId task execution id
     */
    public void deleteTaskExecution(final Long taskExecId) {
        restTemplate.delete(baseURL
                + "task/execution/delete/{execId}", taskExecId);
    }
}
