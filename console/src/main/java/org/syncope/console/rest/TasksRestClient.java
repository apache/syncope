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
import org.syncope.client.to.TaskExecutionTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.TaskExecutionStatus;

/**
 * Console client for invoking Rest Tasks services.
 */
public class TasksRestClient {

    RestClient restClient;

    /**
     * Get all stored tasks.
     * @return list of TaskTO objects
     */
    public List<TaskTO> getAllTasks() {

        List<TaskTO> tasks = Arrays.asList(
                restClient.getRestTemplate().getForObject(
                restClient.getBaseURL() + "task/list.json",
                TaskTO[].class));

        return tasks;

    }

   /**
     * Load an existent task.
     * @return TaskTO object if the configuration exists, null otherwise
     */
    public TaskTO readTask(String taskId) {

        TaskTO taskTO;
        try {
            taskTO = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL() + "task/read/{taskId}",
                    TaskTO.class, taskId);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
            return null;
        }

        return taskTO;
    }

    /**
     * Get all executions.
     * @return list of all executions
     */
    public List<TaskExecutionTO> listExecutions() {
        List<TaskExecutionTO> executions = Arrays.asList(
                restClient.getRestTemplate().getForObject(
                restClient.getBaseURL() + "task/execution/list",
                TaskExecutionTO[].class));

        return executions;
    }

    /**
     * Delete specified task.
     * @param task to delete id
     */
    public void deleteTask(Long taskId) {

        try {
         restClient.getRestTemplate().delete(
                 restClient.getBaseURL() + "task/delete/{taskId}", taskId);
         } catch (SyncopeClientCompositeErrorException scce) {
             scce.printStackTrace();
             throw scce;
         }

    }

    /**
     * Start execution for the specified TaskTO.
     * @param taskTO's id
     * @return boolean: TRUE the operation is executed succesfully,
     *                  FALSE otherwise
     */
    public boolean startTaskExecution(Long taskId) {
        TaskExecutionTO execution;

         try {
            execution = restClient.getRestTemplate().getForObject(
            restClient.getBaseURL() + "task/execute/{taskId}",
            TaskExecutionTO.class, taskId);
         } catch (SyncopeClientCompositeErrorException scce) {
             scce.printStackTrace();
             throw scce;
         }

        return (execution.getStatus() == TaskExecutionStatus.SUBMITTED);
    }

    /**
     * Delete specified task's execution.
     * @param task to delete id
     */
    public boolean deleteTaskExecution(Long execId) {

         try {
                restClient.getRestTemplate().delete(restClient.getBaseURL() +
                        "task/execution/delete/{execId}", execId);
         } catch (SyncopeClientCompositeErrorException scce) {
             scce.printStackTrace();
             throw scce;
         }

        return true;
        }

    /**
     * Getter for restClient attribute.
     * @return RestClient instance
     */
    public RestClient getRestClient() {
        return restClient;
    }

    /**
     * Setter for restClient attribute.
     * @param restClient instance
     */
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}