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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.validation.SyncopeClientException;
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
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class TaskController extends AbstractTransactionalController<AbstractTaskTO> {

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

    @PreAuthorize("hasRole('TASK_CREATE')")
    public <T extends SchedTaskTO> T createSchedTask(final T taskTO) {
        LOG.debug("Creating task " + taskTO);

        TaskUtil taskUtil = TaskUtil.getInstance(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getId(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    public SyncTaskTO updateSync(final SyncTaskTO taskTO) {
        return updateSched(taskTO);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    public <T extends SchedTaskTO> T updateSched(final SchedTaskTO taskTO) {
        LOG.debug("Task update called with parameter {}", taskTO);

        SchedTask task = taskDAO.find(taskTO.getId());
        if (task == null) {
            throw new NotFoundException("Task " + taskTO.getId());
        }

        TaskUtil taskUtil = TaskUtil.getInstance(task);

        binder.updateSchedTask(task, taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getId(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    public int count(final TaskType taskType) {
        return taskDAO.count(TaskUtil.getInstance(taskType).taskClass());
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(final TaskType taskType) {
        TaskUtil taskUtil = TaskUtil.getInstance(taskType);

        List<Task> tasks = taskDAO.findAll(taskUtil.taskClass());
        List<T> taskTOs = new ArrayList<T>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add((T) binder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(final TaskType taskType, final int page, final int size) {
        TaskUtil taskUtil = TaskUtil.getInstance(taskType);

        List<Task> tasks = taskDAO.findAll(page, size, taskUtil.taskClass());
        List<T> taskTOs = new ArrayList<T>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add((T) binder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    public Set<String> getJobClasses() {
        return classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.TASKJOB);
        }

    @PreAuthorize("hasRole('TASK_LIST')")
    public Set<String> getSyncActionsClasses() {
        return classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_ACTIONS);
    }

    @PreAuthorize("hasRole('TASK_READ')")
    public <T extends AbstractTaskTO> T read(final Long taskId) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        return binder.getTaskTO(task, TaskUtil.getInstance(task));
}

    @PreAuthorize("hasRole('TASK_READ')")
    public TaskExecTO readExecution(final Long executionId) {
        TaskExec taskExec = taskExecDAO.find(executionId);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }
        return binder.getTaskExecTO(taskExec);
    }

    @PreAuthorize("hasRole('TASK_EXECUTE')")
    public TaskExecTO execute(final Long taskId, final boolean dryRun) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtil taskUtil = TaskUtil.getInstance(task);

        TaskExecTO result = null;
        LOG.debug("Execution started for {}", task);
        switch (taskUtil.getType()) {
            case PROPAGATION:
                final TaskExec propExec = taskExecutor.execute((PropagationTask) task);
                result = binder.getTaskExecTO(propExec);
                break;

            case NOTIFICATION:
                final TaskExec notExec = notificationJob.executeSingle((NotificationTask) task);
                result = binder.getTaskExecTO(notExec);
                break;

            case SCHEDULED:
            case SYNCHRONIZATION:
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

                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add(e.getMessage());
                    throw sce;
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
        return result;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    public TaskExecTO report(final Long executionId, final PropagationTaskExecStatus status, final String message) {
        TaskExec exec = taskExecDAO.find(executionId);
        if (exec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        SyncopeClientException sce = SyncopeClientException.build(
                ClientExceptionType.InvalidPropagationTaskExecReport);

        TaskUtil taskUtil = TaskUtil.getInstance(exec.getTask());
        if (TaskType.PROPAGATION == taskUtil.getType()) {
            PropagationTask task = (PropagationTask) exec.getTask();
            if (task.getPropagationMode() != PropagationMode.TWO_PHASES) {
                sce.getElements().add("Propagation mode: " + task.getPropagationMode());
            }
        } else {
            sce.getElements().add("Task type: " + taskUtil);
        }

        switch (status) {
            case SUCCESS:
            case FAILURE:
                break;

            case CREATED:
            case SUBMITTED:
            case UNSUBMITTED:
                sce.getElements().add("Execution status to be set: " + status);
                break;

            default:
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        exec.setStatus(status.toString());
        exec.setMessage(message);
        return binder.getTaskExecTO(taskExecDAO.save(exec));
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    public <T extends AbstractTaskTO> T delete(final Long taskId) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        TaskUtil taskUtil = TaskUtil.getInstance(task);

        T taskToDelete = binder.getTaskTO(task, taskUtil);

        if (TaskType.SCHEDULED == taskUtil.getType() || TaskType.SYNCHRONIZATION == taskUtil.getType()) {
            jobInstanceLoader.unregisterJob(task);
        }

        taskDAO.delete(task);
        return taskToDelete;
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    public TaskExecTO deleteExecution(final Long executionId) {
        TaskExec taskExec = taskExecDAO.find(executionId);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        TaskExecTO taskExecutionToDelete = binder.getTaskExecTO(taskExec);
        taskExecDAO.delete(taskExec);
        return taskExecutionToDelete;
    }

    @PreAuthorize("(hasRole('TASK_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE) or "
            + "(hasRole('TASK_EXECUTE') and "
            + "(#bulkAction.operation == #bulkAction.operation.EXECUTE or "
            + "#bulkAction.operation == #bulkAction.operation.DRYRUN))")
    public BulkActionRes bulk(final BulkAction bulkAction) {
        LOG.debug("Bulk '{}' called on '{}'", bulkAction.getOperation(), bulkAction.getTargets());

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractTaskTO resolveReference(final Method method, final Object... args) throws
            UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)
                && !"deleteExecution".equals(method.getName()) && !"readExecution".equals(method.getName())) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof AbstractTaskTO) {
                    id = ((AbstractTaskTO) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                final Task task = taskDAO.find(id);
                return binder.getTaskTO(task, TaskUtil.getInstance(task));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
