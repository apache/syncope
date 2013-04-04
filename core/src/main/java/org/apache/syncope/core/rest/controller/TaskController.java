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
import javax.servlet.http.HttpServletResponse;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditElements.TaskSubCategory;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.init.JobInstanceLoader;
import org.apache.syncope.core.notification.NotificationJob;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.quartz.AbstractTaskJob;
import org.apache.syncope.core.rest.data.TaskDataBinder;
import org.apache.syncope.core.util.TaskUtil;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
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
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationJob notificationJob;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @RequestMapping(method = RequestMethod.POST, value = "/create/sync")
    public TaskTO createSyncTask(final HttpServletResponse response, @RequestBody final SyncTaskTO taskTO) {
        return createSchedTask(response, taskTO);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create/sched")
    public TaskTO createSchedTask(final HttpServletResponse response, @RequestBody final SchedTaskTO taskTO) {
        TaskTO createdTaskTO = createSchedTaskInternal(taskTO);
        response.setStatus(HttpServletResponse.SC_CREATED);
        return createdTaskTO;
    }

    @PreAuthorize("hasRole('TASK_CREATE')")
    public TaskTO createSchedTaskInternal(final SchedTaskTO taskTO) {
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

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update/sync")
    public TaskTO updateSync(@RequestBody final SyncTaskTO taskTO) {
        return updateSched(taskTO);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update/sched")
    public TaskTO updateSched(@RequestBody final SchedTaskTO taskTO) {
        LOG.debug("Task update called with parameter {}", taskTO);

        SchedTask task = taskDAO.find(taskTO.getId());
        if (task == null) {
            throw new NotFoundException("Task " + taskTO.getId());
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

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/count")
    public ModelAndView count(@PathVariable("kind") final String kind) {
        return new ModelAndView().addObject(countInternal(kind));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    public int countInternal(final String kind) {
        return taskDAO.count(getTaskUtil(kind).taskClass());
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
    @RequestMapping(method = RequestMethod.GET, value = "/jobClasses")
    public ModelAndView getJobClasses() {
        Set<String> jobClasses = classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.JOB);

        auditManager.audit(Category.task, TaskSubCategory.getJobClasses, Result.success,
                "Successfully listed all Job classes: " + jobClasses.size());

        return new ModelAndView().addObject(jobClasses);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/syncActionsClasses")
    public ModelAndView getSyncActionsClasses() {
        Set<String> actionsClasses = classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_ACTIONS);

        auditManager.audit(Category.task, TaskSubCategory.getSyncActionsClasses, Result.success,
                "Successfully listed all SyncActions classes: " + actionsClasses.size());

        return new ModelAndView().addObject(actionsClasses);
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{taskId}")
    public TaskTO read(@PathVariable("taskId") final Long taskId) {
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
    public TaskExecTO readExecution(@PathVariable("executionId") final Long executionId) {
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
            @RequestParam(value = "dryRun", defaultValue = "false") final boolean dryRun) {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtil taskUtil = getTaskUtil(task);

        TaskExecTO result = null;
        LOG.debug("Execution started for {}", task);
        switch (taskUtil) {
            case PROPAGATION:
                final TaskExec propExec = taskExecutor.execute((PropagationTask) task);
                result = binder.getTaskExecTO(propExec);
                break;

            case NOTIFICATION:
                final TaskExec notExec = notificationJob.executeSingle((NotificationTask) task);
                result = binder.getTaskExecTO(notExec);
                break;

            case SCHED:
            case SYNC:
                try {
                    jobInstanceLoader.registerJob(task,
                            ((SchedTask) task).getJobClassName(),
                            ((SchedTask) task).getCronExpression());

                    JobDataMap map = new JobDataMap();
                    map.put(AbstractTaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);

                    scheduler.getScheduler().triggerJob(
                            new JobKey(JobInstanceLoader.getJobName(task), Scheduler.DEFAULT_GROUP), map);
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
            @RequestParam("message") final String message) {

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
    public TaskTO delete(@PathVariable("taskId") final Long taskId) {
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
    public TaskExecTO deleteExecution(@PathVariable("executionId") final Long executionId) {
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

    @PreAuthorize("(hasRole('TASK_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE) or "
            + "(hasRole('TASK_EXECUTE') and "
            + "(#bulkAction.operation == #bulkAction.operation.EXECUTE or "
            + "#bulkAction.operation == #bulkAction.operation.DRYRUN))")
    @RequestMapping(method = RequestMethod.POST, value = "/bulk")
    public BulkActionRes bulkAction(@RequestBody final BulkAction bulkAction) {
        LOG.debug("Bulk action '{}' called on '{}'", bulkAction.getOperation(), bulkAction.getTargets());

        BulkActionRes res = new BulkActionRes();

        switch (bulkAction.getOperation()) {
            case DELETE:
                for (String taskId : bulkAction.getTargets()) {
                    try {
                        res.add(delete(Long.valueOf(taskId)).getId(), BulkActionRes.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for task {}", taskId, e);
                        res.add(taskId, BulkActionRes.Status.FAILURE);
                    }
                }
                break;
            case DRYRUN:
                for (String taskId : bulkAction.getTargets()) {
                    try {
                        execute(Long.valueOf(taskId), true);
                        res.add(taskId, BulkActionRes.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing dryrun for task {}", taskId, e);
                        res.add(taskId, BulkActionRes.Status.FAILURE);
                    }
                }
                break;
            case EXECUTE:
                for (String taskId : bulkAction.getTargets()) {
                    try {
                        execute(Long.valueOf(taskId), false);
                        res.add(taskId, BulkActionRes.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing execute for task {}", taskId, e);
                        res.add(taskId, BulkActionRes.Status.FAILURE);
                    }
                }
                break;
            default:
        }

        return res;
    }
}
