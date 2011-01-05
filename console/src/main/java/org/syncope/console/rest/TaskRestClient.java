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
import org.syncope.types.TaskExecutionStatus;

/**
 * Console client for invoking Rest Tasks services.
 */
@Component
public class TaskRestClient extends AbstractBaseRestClient {

    /**
     * Get all stored tasks.
     * @return list of TaskTO objects
     */
    public List<TaskTO> getAllTasks() {

        List<TaskTO> tasks = Arrays.asList(
                restTemplate.getForObject(
                baseURL + "task/list.json",
                TaskTO[].class));

        return tasks;

    }

    /**
     * Load an existent task.
     * @return TaskTO object if the configuration exists, null otherwise
     */
    public TaskTO readTask(String taskId) {

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
        List<TaskExecutionTO> executions = Arrays.asList(
                restTemplate.getForObject(
                baseURL + "task/execution/list",
                TaskExecutionTO[].class));

        return executions;
    }

    /**
     * Delete specified task.
     * @param task to delete id
     */
    public void deleteTask(Long taskId) {

        try {
            restTemplate.delete(
                    baseURL + "task/delete/{taskId}", taskId);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a task", e);
        }
    }

    /**
     * Start execution for the specified TaskTO.
     * @param taskTO's id
     * @return boolean: TRUE the operation is executed succesfully,
     *                  FALSE otherwise
     */
    public boolean startTaskExecution(Long taskId) {
        boolean result = false;
        try {
            TaskExecutionTO execution = restTemplate.getForObject(
                    baseURL + "task/execute/{taskId}",
                    TaskExecutionTO.class, taskId);
            result = (execution.getStatus() == TaskExecutionStatus.SUBMITTED);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While starting a task", e);
        }

        return result;
    }

    /**
     * Delete specified task's execution.
     * @param task to delete id
     */
    public boolean deleteTaskExecution(Long execId) {

        try {
            restTemplate.delete(baseURL
                    + "task/execution/delete/{execId}", execId);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deletring a task execution", e);
        }

        return true;
    }
}
