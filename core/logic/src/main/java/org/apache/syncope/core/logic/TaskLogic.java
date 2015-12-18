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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractExecTO;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.job.JobInstanceLoader;
import org.apache.syncope.core.logic.notification.NotificationJobDelegate;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class TaskLogic extends AbstractJobLogic<AbstractTaskTO> {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private TaskDataBinder binder;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationJobDelegate notificationJobDelegate;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_CREATE + "')")
    public <T extends SchedTaskTO> T createSchedTask(final T taskTO) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(
                    task,
                    task.getStartAt(),
                    confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).getLongValue());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils, false);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_UPDATE + "')")
    public SyncTaskTO updateSync(final SyncTaskTO taskTO) {
        return updateSched(taskTO);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_UPDATE + "')")
    public <T extends SchedTaskTO> T updateSched(final SchedTaskTO taskTO) {
        SchedTask task = taskDAO.find(taskTO.getKey());
        if (task == null) {
            throw new NotFoundException("Task " + taskTO.getKey());
        }

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        binder.updateSchedTask(task, taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(
                    task,
                    task.getStartAt(),
                    confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).getLongValue());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils, false);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_LIST + "')")
    public int count(
            final TaskType type, final String resource, final AnyTypeKind anyTypeKind, final Long anyTypeKey) {

        return taskDAO.count(type, resourceDAO.find(resource), anyTypeKind, anyTypeKey);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_LIST + "')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(
            final TaskType type, final String resource, final AnyTypeKind anyTypeKind, final Long anyTypeKey,
            final int page, final int size, final List<OrderByClause> orderByClauses, final boolean details) {

        final TaskUtils taskUtils = taskUtilsFactory.getInstance(type);

        return CollectionUtils.collect(taskDAO.findAll(
                type, resourceDAO.find(resource), anyTypeKind, anyTypeKey, page, size, orderByClauses),
                new Transformer<Task, T>() {

            @Override
            public T transform(final Task task) {
                return (T) binder.getTaskTO(task, taskUtils, details);
            }
        }, new ArrayList<T>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_READ + "')")
    public <T extends AbstractTaskTO> T read(final Long taskKey, final boolean details) {
        Task task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        }
        return binder.getTaskTO(task, taskUtilsFactory.getInstance(task), details);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    public TaskExecTO execute(final Long taskKey, final Date startAt, final boolean dryRun) {
        Task task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        }
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        TaskExecTO result = null;
        switch (taskUtils.getType()) {
            case PROPAGATION:
                TaskExec propExec = taskExecutor.execute((PropagationTask) task);
                result = binder.getTaskExecTO(propExec);
                break;

            case NOTIFICATION:
                TaskExec notExec = notificationJobDelegate.executeSingle((NotificationTask) task);
                result = binder.getTaskExecTO(notExec);
                break;

            case SCHEDULED:
            case SYNCHRONIZATION:
            case PUSH:
                if (!((SchedTask) task).isActive()) {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add("Task " + taskKey + " is not active");
                    throw sce;
                }

                try {
                    Map<String, Object> jobDataMap = jobInstanceLoader.registerJob(
                            (SchedTask) task,
                            startAt,
                            confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).getLongValue());

                    jobDataMap.put(TaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);

                    if (startAt == null) {
                        scheduler.getScheduler().triggerJob(
                                new JobKey(JobNamer.getJobName(task), Scheduler.DEFAULT_GROUP),
                                new JobDataMap(jobDataMap));
                    }
                } catch (Exception e) {
                    LOG.error("While executing task {}", task, e);

                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add(e.getMessage());
                    throw sce;
                }

                result = new TaskExecTO();
                result.setTask(taskKey);
                result.setStart(new Date());
                result.setStatus("JOB_FIRED");
                result.setMessage("Job fired; waiting for results...");
                break;

            default:
        }

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_DELETE + "')")
    public <T extends AbstractTaskTO> T delete(final Long taskKey) {
        Task task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        }
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        T taskToDelete = binder.getTaskTO(task, taskUtils, true);

        if (TaskType.SCHEDULED == taskUtils.getType()
                || TaskType.SYNCHRONIZATION == taskUtils.getType()
                || TaskType.PUSH == taskUtils.getType()) {

            jobInstanceLoader.unregisterJob(task);
        }

        taskDAO.delete(task);
        return taskToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_READ + "')")
    public int countExecutions(final Long taskId) {
        return taskExecDAO.count(taskId);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_READ + "')")
    public List<TaskExecTO> listExecutions(
            final Long taskKey, final int page, final int size, final List<OrderByClause> orderByClauses) {

        Task task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        }

        return CollectionUtils.collect(taskExecDAO.findAll(task, page, size, orderByClauses),
                new Transformer<TaskExec, TaskExecTO>() {

            @Override
            public TaskExecTO transform(final TaskExec taskExec) {
                return binder.getTaskExecTO(taskExec);
            }
        }, new ArrayList<TaskExecTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_DELETE + "')")
    public TaskExecTO deleteExecution(final Long execKey) {
        TaskExec taskExec = taskExecDAO.find(execKey);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + execKey);
        }

        TaskExecTO taskExecutionToDelete = binder.getTaskExecTO(taskExec);
        taskExecDAO.delete(taskExec);
        return taskExecutionToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_DELETE + "')")
    public BulkActionResult deleteExecutions(
            final Long taskKey,
            final Date startedBefore, final Date startedAfter, final Date endedBefore, final Date endedAfter) {

        Task task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        }

        BulkActionResult result = new BulkActionResult();

        for (TaskExec exec : taskExecDAO.findAll(task, startedBefore, startedAfter, endedBefore, endedAfter)) {
            try {
                taskExecDAO.delete(exec);
                result.getResults().put(String.valueOf(exec.getKey()), BulkActionResult.Status.SUCCESS);
            } catch (Exception e) {
                LOG.error("Error deleting execution {} of task {}", exec.getKey(), taskKey, e);
                result.getResults().put(String.valueOf(exec.getKey()), BulkActionResult.Status.FAILURE);
            }
        }

        return result;
    }

    @Override
    protected Long getKeyFromJobName(final JobKey jobKey) {
        return JobNamer.getTaskKeyFromJobName(jobKey.getName());
    }

    @Override
    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_LIST + "')")
    public <E extends AbstractExecTO> List<E> listJobs(final JobStatusType type, final Class<E> reference) {
        return super.listJobs(type, reference);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    public void actionJob(final Long taskKey, final JobAction action) {
        Task task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        }
        String jobName = JobNamer.getJobName(task);
        actionJob(jobName, action);
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

        if ((key != null) && !key.equals(0L)) {
            try {
                final Task task = taskDAO.find(key);
                return binder.getTaskTO(task, taskUtilsFactory.getInstance(task), false);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
