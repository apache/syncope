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
package org.syncope.core.rest.data;

import com.opensymphony.workflow.Workflow;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.syncope.client.to.TaskExecutionTO;
import org.syncope.client.to.TaskTO;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.workflow.WFUtils;

@Component
public class TaskDataBinder {

    private static final String[] IGNORE_TASK_PROPERTIES = {
        "executions", "resource"};

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = {
        "task"};

    public TaskExecutionTO getTaskExecutionTO(final Workflow workflow,
            final TaskExecution execution) {

        TaskExecutionTO executionTO = new TaskExecutionTO();
        BeanUtils.copyProperties(execution, executionTO,
                IGNORE_TASK_EXECUTION_PROPERTIES);
        executionTO.setStatus(
                WFUtils.getTaskExecutionStatus(workflow, execution));
        executionTO.setTask(execution.getTask().getId());

        return executionTO;
    }

    public TaskTO getTaskTO(final Workflow workflow, final Task task) {
        TaskTO taskTO = new TaskTO();
        BeanUtils.copyProperties(task, taskTO, IGNORE_TASK_PROPERTIES);

        for (TaskExecution execution : task.getExecutions()) {
            taskTO.addExecution(getTaskExecutionTO(workflow, execution));
        }

        taskTO.setResource(task.getResource().getName());

        return taskTO;
    }
}
