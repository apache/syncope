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
package org.apache.syncope.server.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.dao.TaskDAO;
import org.apache.syncope.server.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.server.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.server.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.server.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.server.persistence.api.entity.task.SchedTask;
import org.apache.syncope.server.persistence.api.entity.task.Task;
import org.apache.syncope.server.persistence.api.entity.task.TaskExec;
import org.apache.syncope.server.persistence.api.entity.task.TaskUtil;
import org.apache.syncope.server.persistence.api.entity.task.TaskUtilFactory;
import org.apache.syncope.server.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.server.provisioning.api.job.JobNamer;
import org.apache.syncope.server.provisioning.api.job.TaskJob;
import org.apache.syncope.server.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.server.provisioning.api.job.JobInstanceLoader;
import org.apache.syncope.server.logic.notification.NotificationJob;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class TaskLogic extends AbstractTransactionalLogic<AbstractTaskTO> {

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
    private TaskUtilFactory taskUtilFactory;

    @PreAuthorize("hasRole('TASK_CREATE')")
    public <T extends SchedTaskTO> T createSchedTask(final T taskTO) {
        TaskUtil taskUtil = taskUtilFactory.getInstance(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

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
        SchedTask task = taskDAO.find(taskTO.getKey());
        if (task == null) {
            throw new NotFoundException("Task " + taskTO.getKey());
        }

        TaskUtil taskUtil = taskUtilFactory.getInstance(task);

        binder.updateSchedTask(task, taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(), task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    public int count(final TaskType taskType) {
        return taskDAO.count(taskType);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(final TaskType taskType,
            final int page, final int size, final List<OrderByClause> orderByClauses) {

        TaskUtil taskUtil = taskUtilFactory.getInstance(taskType);

        List<Task> tasks = taskDAO.findAll(page, size, orderByClauses, taskType);
        List<T> taskTOs = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add((T) binder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    public <T extends AbstractTaskTO> T read(final Long taskId) {
        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }
        return binder.getTaskTO(task, taskUtilFactory.getInstance(task));
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
        TaskUtil taskUtil = taskUtilFactory.getInstance(task);

        TaskExecTO result = null;
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
            case PUSH:
                try {
                    jobInstanceLoader.registerJob(task,
                            ((SchedTask) task).getJobClassName(),
                            ((SchedTask) task).getCronExpression());

                    JobDataMap map = new JobDataMap();
                    map.put(TaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);

                    scheduler.getScheduler().triggerJob(
                            new JobKey(JobNamer.getJobName(task), Scheduler.DEFAULT_GROUP), map);
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

        TaskUtil taskUtil = taskUtilFactory.getInstance(exec.getTask());
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
        TaskUtil taskUtil = taskUtilFactory.getInstance(task);

        T taskToDelete = binder.getTaskTO(task, taskUtil);

        if (TaskType.SCHEDULED == taskUtil.getType()
                || TaskType.SYNCHRONIZATION == taskUtil.getType()
                || TaskType.PUSH == taskUtil.getType()) {

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
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult res = new BulkActionResult();

        switch (bulkAction.getOperation()) {
            case DELETE:
                for (String taskId : bulkAction.getTargets()) {
                    try {
                        res.add(delete(Long.valueOf(taskId)).getKey(), BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for task {}", taskId, e);
                        res.add(taskId, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            case DRYRUN:
                for (String taskId : bulkAction.getTargets()) {
                    try {
                        execute(Long.valueOf(taskId), true);
                        res.add(taskId, BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing dryrun for task {}", taskId, e);
                        res.add(taskId, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            case EXECUTE:
                for (String taskId : bulkAction.getTargets()) {
                    try {
                        execute(Long.valueOf(taskId), false);
                        res.add(taskId, BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing execute for task {}", taskId, e);
                        res.add(taskId, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            default:
        }

        return res;
    }

    @Override
    protected AbstractTaskTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long key = null;

        if (ArrayUtils.isNotEmpty(args)
                && !"deleteExecution".equals(method.getName()) && !"readExecution".equals(method.getName())) {

            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof AbstractTaskTO) {
                    key = ((AbstractTaskTO) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0l)) {
            try {
                final Task task = taskDAO.find(key);
                return binder.getTaskTO(task, taskUtilFactory.getInstance(task));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
