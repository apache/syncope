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
package org.syncope.core.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.syncope.client.to.TaskExecutionTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecutionDAO;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.rest.data.TaskDataBinder;
import org.syncope.types.PropagationMode;
import org.syncope.types.SyncopeClientExceptionType;
import org.syncope.types.TaskExecutionStatus;

@Controller
@RequestMapping("/task")
public class TaskController extends AbstractController {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecutionDAO taskExecutionDAO;

    @Autowired
    private TaskDataBinder taskDataBinder;

    @Autowired
    private PropagationManager propagationManager;

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<TaskTO> list() {
        List<Task> tasks = taskDAO.findAll();
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(taskDataBinder.getTaskTO(task));
        }

        return taskTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/list")
    public List<TaskExecutionTO> listExecutions() {
        List<TaskExecution> executions = taskExecutionDAO.findAll();
        List<TaskExecutionTO> executionTOs =
                new ArrayList<TaskExecutionTO>(executions.size());
        for (TaskExecution execution : executions) {
            executionTOs.add(taskDataBinder.getTaskExecutionTO(execution));
        }

        return executionTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{taskId}")
    public TaskTO read(@PathVariable("taskId") final Long taskId)
            throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        return taskDataBinder.getTaskTO(task);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/read/{executionId}")
    public TaskExecutionTO readExecution(
            @PathVariable("executionId") final Long executionId)
            throws NotFoundException {

        TaskExecution execution = taskExecutionDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        return taskDataBinder.getTaskExecutionTO(execution);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/execute/{taskId}")
    public TaskExecutionTO execute(@PathVariable("taskId") final Long taskId)
            throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        TaskExecution execution = new TaskExecution();
        execution.setTask(task);
        task.addExecution(execution);
        execution = taskExecutionDAO.save(execution);

        LOG.debug("Execution started for {}", task);

        if (PropagationMode.SYNC
                == execution.getTask().getPropagationMode()) {

            propagationManager.syncPropagate(execution);
        } else {
            propagationManager.asyncPropagate(execution);
        }

        LOG.debug("Execution finished for {}", task);

        execution = taskExecutionDAO.save(execution);

        return taskDataBinder.getTaskExecutionTO(execution);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/report/{executionId}")
    public TaskExecutionTO report(
            @PathVariable("executionId") final Long executionId,
            @RequestParam("executionStatus") final TaskExecutionStatus status,
            @RequestParam("message") final String message)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExecution execution = taskExecutionDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        SyncopeClientException invalidReportException =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidTaskExecutionReport);
        if (execution.getTask().getPropagationMode() != PropagationMode.ASYNC) {
            invalidReportException.addElement("Propagation mode: "
                    + execution.getTask().getPropagationMode().toString());
        }
        if (execution.getStatus() != TaskExecutionStatus.SUBMITTED) {
            invalidReportException.addElement("Current execution status: "
                    + execution.getStatus().toString());
        }
        if (status != TaskExecutionStatus.SUCCESS
                && status != TaskExecutionStatus.FAILURE) {

            invalidReportException.addElement("Execution status to be set: "
                    + status.toString());
        }
        if (!invalidReportException.getElements().isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(invalidReportException);
            throw scce;
        }

        execution.setStatus(status);
        execution.setMessage(message);
        execution = taskExecutionDAO.save(execution);

        return taskDataBinder.getTaskExecutionTO(execution);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{taskId}")
    public void delete(@PathVariable("taskId") Long taskId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        SyncopeClientException incompleteTaskExecution =
                new SyncopeClientException(
                SyncopeClientExceptionType.IncompleteTaskExecution);

        for (TaskExecution execution : task.getExecutions()) {
            if (execution.getStatus() == TaskExecutionStatus.SUBMITTED) {
                incompleteTaskExecution.addElement(
                        execution.getId().toString());
            }
        }
        if (!incompleteTaskExecution.getElements().isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(incompleteTaskExecution);
            throw scce;
        }

        taskDAO.delete(task);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/execution/delete/{executionId}")
    public void deleteExecution(@PathVariable("executionId") Long executionId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExecution execution = taskExecutionDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        if (execution.getStatus() == TaskExecutionStatus.SUBMITTED) {
            SyncopeClientException incompleteTaskExecution =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.IncompleteTaskExecution);
            incompleteTaskExecution.addElement(
                    execution.getId().toString());

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(incompleteTaskExecution);
            throw scce;
        }

        taskExecutionDAO.delete(execution);
    }
}
