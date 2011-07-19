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

import com.opensymphony.workflow.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.rest.data.TaskDataBinder;
import org.syncope.core.util.TaskUtil;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationTaskExecStatus;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/task")
public class TaskController extends AbstractController {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDataBinder taskDataBinder;

    @Autowired
    private PropagationManager propagationManager;

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/count")
    public ModelAndView count(@PathVariable("kind") final String kind) {
        return new ModelAndView().addObject(
                taskDAO.count(getTaskUtil(kind).taskClass()));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/list")
    public List<TaskTO> list(@PathVariable("kind") final String kind) {
        TaskUtil taskUtil = getTaskUtil(kind);

        List<Task> tasks = taskDAO.findAll(taskUtil.taskClass());
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(taskDataBinder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/list/{page}/{size}")
    public List<TaskTO> list(
            @PathVariable("kind") final String kind,
            @PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        TaskUtil taskUtil = getTaskUtil(kind);

        List<Task> tasks = taskDAO.findAll(page, size, taskUtil.taskClass());
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(taskDataBinder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/execution/list")
    public List<TaskExecTO> listExecutions(
            @PathVariable("kind") final String kind) {

        List<TaskExec> executions = taskExecDAO.findAll(
                getTaskUtil(kind).taskClass());
        List<TaskExecTO> executionTOs =
                new ArrayList<TaskExecTO>(executions.size());
        for (TaskExec execution : executions) {
            executionTOs.add(
                    taskDataBinder.getTaskExecutionTO(execution));
        }

        return executionTOs;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{taskId}")
    public TaskTO read(@PathVariable("taskId") final Long taskId)
            throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        return taskDataBinder.getTaskTO(task, getTaskUtil(task));
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/read/{executionId}")
    public TaskExecTO readExecution(
            @PathVariable("executionId") final Long executionId)
            throws NotFoundException {

        TaskExec execution = taskExecDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        return taskDataBinder.getTaskExecutionTO(execution);
    }

    @PreAuthorize("hasRole('TASK_EXECUTE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execute/{taskId}")
    public TaskExecTO execute(
            @PathVariable("taskId") final Long taskId)
            throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        TaskExec execution = new TaskExec();
        execution.setTask(task);
        switch (getTaskUtil(task)) {
            case PROPAGATION:
                execution.setStatus(
                        PropagationTaskExecStatus.CREATED.toString());
                execution = taskExecDAO.save(execution);

                LOG.debug("Execution started for {}", task);
                propagationManager.propagate(execution);
                LOG.debug("Execution finished for {}, {}", task, execution);
                break;

            case SCHED:
                break;

            case SYNC:
                break;
        }

        return taskDataBinder.getTaskExecutionTO(execution);
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/report/{executionId}")
    public TaskExecTO report(
            @PathVariable("executionId") final Long executionId,
            @RequestParam("executionStatus")
            final PropagationTaskExecStatus status,
            @RequestParam("message") final String message)
            throws NotFoundException, SyncopeClientCompositeErrorException,
            WorkflowException {

        TaskExec exec = taskExecDAO.find(executionId);
        if (exec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        SyncopeClientException invalidReportException =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidPropagationTaskExecReport);

        TaskUtil taskUtil = getTaskUtil(exec.getTask());
        if (taskUtil != TaskUtil.PROPAGATION) {
            invalidReportException.addElement("Task type: " + taskUtil);
        } else {
            PropagationTask task = (PropagationTask) exec.getTask();
            if (task.getPropagationMode() != PropagationMode.ASYNC) {
                invalidReportException.addElement(
                        "Propagation mode: " + task.getPropagationMode());
            }
        }

        switch (status) {
            case SUCCESS:
            case FAILURE:
                break;

            case CREATED:
            case SUBMITTED:
            case UNSUBMITTED:
                invalidReportException.addElement(
                        "Execution status to be set: " + status);
                break;

            default:
        }

        if (!invalidReportException.getElements().isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(invalidReportException);
            throw scce;
        }

        exec.setStatus(status.toString());
        exec.setMessage(message);
        exec = taskExecDAO.save(exec);

        return taskDataBinder.getTaskExecutionTO(exec);
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{taskId}")
    public void delete(@PathVariable("taskId") Long taskId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        TaskUtil taskUtil = getTaskUtil(task);
        if (taskUtil == TaskUtil.PROPAGATION) {
            SyncopeClientException incompleteTaskExecution =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.IncompletePropagationTaskExec);

            List<TaskExec> execs = task.getExecs();
            for (TaskExec exec : execs) {
                if (PropagationTaskExecStatus.CREATED.toString().equals(
                        exec.getStatus())
                        || PropagationTaskExecStatus.SUBMITTED.toString().equals(
                        exec.getStatus())) {

                    incompleteTaskExecution.addElement(exec.getId().toString());
                }
            }
            if (!incompleteTaskExecution.getElements().isEmpty()) {
                SyncopeClientCompositeErrorException scce =
                        new SyncopeClientCompositeErrorException(
                        HttpStatus.BAD_REQUEST);
                scce.addException(incompleteTaskExecution);
                throw scce;
            }
        }

        taskDAO.delete(task);
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/execution/delete/{executionId}")
    public void deleteExecution(@PathVariable("executionId") Long executionId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExec execution = taskExecDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        if (PropagationTaskExecStatus.CREATED.toString().equals(
                execution.getStatus())
                || PropagationTaskExecStatus.SUBMITTED.toString().equals(
                execution.getStatus())) {

            SyncopeClientException incompleteTaskExecution =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.IncompletePropagationTaskExec);
            incompleteTaskExecution.addElement(
                    execution.getId().toString());

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(incompleteTaskExecution);
            throw scce;
        }

        taskExecDAO.delete(execution);
    }
}
