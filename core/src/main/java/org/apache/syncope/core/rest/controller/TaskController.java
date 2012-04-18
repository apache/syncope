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
package org.apache.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.apache.syncope.client.to.SchedTaskTO;
import org.apache.syncope.client.to.SyncTaskTO;
import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.init.JobInstanceLoader;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.rest.data.TaskDataBinder;
import org.apache.syncope.core.scheduling.AbstractTaskJob;
import org.apache.syncope.core.util.TaskUtil;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.TaskSubCategory;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/task")
public class TaskController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDataBinder binder;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @PreAuthorize("hasRole('TASK_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create/sync")
    public TaskTO createSyncTask(final HttpServletResponse response, @RequestBody final SyncTaskTO taskTO)
            throws NotFoundException {

        return createSchedTask(response, taskTO);
    }

    @PreAuthorize("hasRole('TASK_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create/sched")
    public TaskTO createSchedTask(final HttpServletResponse response, @RequestBody final SchedTaskTO taskTO)
            throws NotFoundException {

        LOG.debug("Creating task " + taskTO);

        TaskUtil taskUtil = getTaskUtil(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getId(), e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        auditManager.audit(Category.task, TaskSubCategory.create, Result.success,
                "Successfully created task: " + task.getId() + "/" + taskUtil);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update/sync")
    public TaskTO updateSync(@RequestBody final SyncTaskTO taskTO) throws NotFoundException {

        return updateSched(taskTO);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update/sched")
    public TaskTO updateSched(@RequestBody final SchedTaskTO taskTO) throws NotFoundException {

        LOG.debug("Task update called with parameter {}", taskTO);

        SchedTask task = taskDAO.find(taskTO.getId());
        if (task == null) {
            throw new NotFoundException("Task " + String.valueOf(taskTO.getId()));
        }

        TaskUtil taskUtil = getTaskUtil(task);

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        binder.updateSchedTask(task, taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getId(), e);

            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        auditManager.audit(Category.task, TaskSubCategory.update, Result.success,
                "Successfully udpated task: " + task.getId() + "/" + taskUtil);

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/count")
    public ModelAndView count(@PathVariable("kind") final String kind) {
        return new ModelAndView().addObject(taskDAO.count(getTaskUtil(kind).taskClass()));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<TaskTO> list(@PathVariable("kind") final String kind) {
        TaskUtil taskUtil = getTaskUtil(kind);

        List<Task> tasks = taskDAO.findAll(taskUtil.taskClass());
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(binder.getTaskTO(task, taskUtil));
        }

        auditManager.audit(Category.task, TaskSubCategory.list, Result.success,
                "Successfully listed all tasks: " + taskTOs.size() + "/" + taskUtil);

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list/{page}/{size}")
    public List<TaskTO> list(@PathVariable("kind") final String kind, @PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        TaskUtil taskUtil = getTaskUtil(kind);

        List<Task> tasks = taskDAO.findAll(page, size, taskUtil.taskClass());
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(binder.getTaskTO(task, taskUtil));
        }

        auditManager.audit(Category.task, TaskSubCategory.list, Result.success,
                "Successfully listed all tasks (page=" + page + ", size=" + size + "): "
                + taskTOs.size() + "/" + taskUtil);

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/execution/list")
    public List<TaskExecTO> listExecutions(@PathVariable("kind") final String kind) {

        List<TaskExec> executions = taskExecDAO.findAll(getTaskUtil(kind).taskClass());
        List<TaskExecTO> executionTOs = new ArrayList<TaskExecTO>(executions.size());
        for (TaskExec execution : executions) {
            executionTOs.add(binder.getTaskExecTO(execution));
        }

        auditManager.audit(Category.task, TaskSubCategory.listExecutions, Result.success,
                "Successfully listed all task executions: " + executionTOs.size() + "/" + kind);

        return executionTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/jobClasses")
    public ModelAndView getJobClasses() {
        Set<String> jobClasses = classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.JOB);

        auditManager.audit(Category.task, TaskSubCategory.getJobClasses, Result.success,
                "Successfully listed all Job classes: " + jobClasses.size());

        return new ModelAndView().addObject(jobClasses);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/jobActionsClasses")
    public ModelAndView getJobActionClasses() {
        Set<String> jobActionsClasses = classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.JOB_ACTIONS);

        auditManager.audit(Category.task, TaskSubCategory.getJobActionClasses, Result.success,
                "Successfully listed all SyncJobActions classes: " + jobActionsClasses.size());

        return new ModelAndView().addObject(jobActionsClasses);
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{taskId}")
    public TaskTO read(@PathVariable("taskId") final Long taskId) throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtil taskUtil = getTaskUtil(task);

        auditManager.audit(Category.task, TaskSubCategory.read, Result.success,
                "Successfully read task: " + task.getId() + "/" + taskUtil);

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/read/{executionId}")
    public TaskExecTO readExecution(@PathVariable("executionId") final Long executionId) throws NotFoundException {

        TaskExec taskExec = taskExecDAO.find(executionId);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        auditManager.audit(Category.task, TaskSubCategory.readExecution, Result.success,
                "Successfully read task execution: " + taskExec.getId());

        return binder.getTaskExecTO(taskExec);
    }

    @PreAuthorize("hasRole('TASK_EXECUTE')")
    @RequestMapping(method = RequestMethod.POST, value = "/execute/{taskId}")
    public TaskExecTO execute(@PathVariable("taskId") final Long taskId,
            @RequestParam(value = "dryRun", defaultValue = "false") final boolean dryRun) throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtil taskUtil = getTaskUtil(task);

        TaskExecTO result = null;
        LOG.debug("Execution started for {}", task);
        switch (taskUtil) {
            case PROPAGATION:
                final TaskExec propExec = propagationManager.execute((PropagationTask) task);
                result = binder.getTaskExecTO(propExec);
                break;

            case NOTIFICATION:
                final TaskExec notExec = notificationManager.execute((NotificationTask) task);
                result = binder.getTaskExecTO(notExec);
                break;

            case SCHED:
            case SYNC:
                try {
                    jobInstanceLoader.registerJob(task, ((SchedTask) task).getJobClassName(), ((SchedTask) task).
                            getCronExpression());

                    JobDataMap map = new JobDataMap();
                    map.put(AbstractTaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);
                    scheduler.getScheduler().triggerJob(JobInstanceLoader.getJobName(task), Scheduler.DEFAULT_GROUP,
                            map);
                } catch (Exception e) {
                    LOG.error("While executing task {}", task, e);

                    auditManager.audit(Category.task, TaskSubCategory.execute, Result.failure,
                            "Could not start execution for task: " + task.getId() + "/" + taskUtil, e);

                    SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(
                            HttpStatus.BAD_REQUEST);
                    SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.Scheduling);
                    sce.addElement(e.getMessage());
                    scce.addException(sce);
                    throw scce;
                }

                result = new TaskExecTO();
                result.setTask(taskId);
                result.setStartDate(new Date());
                result.setStatus("JOB_FIRED");
                result.setMessage("Job fired; waiting for results...");
                break;

            default:
        }
        LOG.debug("Execution finished for {}, {}", task, result);

        auditManager.audit(Category.task, TaskSubCategory.execute, Result.success,
                "Successfully started execution for task: " + task.getId() + "/" + taskUtil);

        return result;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/report/{executionId}")
    public TaskExecTO report(@PathVariable("executionId") final Long executionId,
            @RequestParam("executionStatus") final PropagationTaskExecStatus status,
            @RequestParam("message") final String message)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExec exec = taskExecDAO.find(executionId);
        if (exec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        SyncopeClientException sce = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidPropagationTaskExecReport);

        TaskUtil taskUtil = getTaskUtil(exec.getTask());
        if (TaskUtil.PROPAGATION == taskUtil) {
            PropagationTask task = (PropagationTask) exec.getTask();
            if (task.getPropagationMode() != PropagationMode.TWO_PHASES) {
                sce.addElement("Propagation mode: " + task.getPropagationMode());
            }
        } else {
            sce.addElement("Task type: " + taskUtil);
        }

        switch (status) {
            case SUCCESS:
            case FAILURE:
                break;

            case CREATED:
            case SUBMITTED:
            case UNSUBMITTED:
                sce.addElement("Execution status to be set: " + status);
                break;

            default:
        }

        if (!sce.isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            scce.addException(sce);

            auditManager.audit(Category.task, TaskSubCategory.report, Result.failure,
                    "Could not reported execution status: " + exec.getId() + "/" + taskUtil, scce);

            throw scce;
        }

        exec.setStatus(status.toString());
        exec.setMessage(message);
        exec = taskExecDAO.save(exec);

        auditManager.audit(Category.task, TaskSubCategory.report, Result.success,
                "Successfully reported execution status: " + exec.getId() + "/" + taskUtil);

        return binder.getTaskExecTO(exec);
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{taskId}")
    public TaskTO delete(@PathVariable("taskId") final Long taskId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtil taskUtil = getTaskUtil(task);
        
        TaskTO taskToDelete = binder.getTaskTO(task, taskUtil);

        if (TaskUtil.SCHED == taskUtil || TaskUtil.SYNC == taskUtil) {
            jobInstanceLoader.unregisterJob(task);
        }

        taskDAO.delete(task);

        auditManager.audit(Category.task, TaskSubCategory.delete, Result.success,
                "Successfully deleted task: " + task.getId() + "/" + taskUtil);
        
        return taskToDelete;
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/delete/{executionId}")
    public TaskExecTO deleteExecution(@PathVariable("executionId") final Long executionId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExec taskExec = taskExecDAO.find(executionId);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }
        
        TaskExecTO taskExecutionToDelete = binder.getTaskExecTO(taskExec);

        taskExecDAO.delete(taskExec);

        auditManager.audit(Category.task, TaskSubCategory.deleteExecution, Result.success,
                "Successfully deleted task execution: " + taskExec.getId());
        return taskExecutionToDelete;
    }
}
