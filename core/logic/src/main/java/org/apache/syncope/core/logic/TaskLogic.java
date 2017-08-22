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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
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
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJobDelegate;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class TaskLogic extends AbstractExecutableLogic<AbstractTaskTO> {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private TaskDataBinder binder;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationJobDelegate notificationJobDelegate;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_CREATE + "')")
    public <T extends SchedTaskTO> T createSchedTask(final T taskTO) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobManager.register(
                    task,
                    task.getStartAt(),
                    confDAO.find("tasks.interruptMaxRetries", 1L));
        } catch (Exception e) {
            LOG.error("While registering quartz job for task " + task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils, false);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_UPDATE + "')")
    public <T extends SchedTaskTO> T updateSchedTask(final SchedTaskTO taskTO) {
        SchedTask task = taskDAO.find(taskTO.getKey());
        if (task == null) {
            throw new NotFoundException("Task " + taskTO.getKey());
        }

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        binder.updateSchedTask(task, taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobManager.register(
                    task,
                    task.getStartAt(),
                    confDAO.find("tasks.interruptMaxRetries", 1L));
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
            final TaskType type,
            final String resource,
            final String notification,
            final AnyTypeKind anyTypeKind,
            final String anyTypeKey) {

        return taskDAO.count(
                type, resourceDAO.find(resource), notificationDAO.find(notification), anyTypeKind, anyTypeKey);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_LIST + "')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> List<T> list(
            final TaskType type,
            final String resource,
            final String notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses,
            final boolean details) {

        return taskDAO.findAll(
                type, resourceDAO.find(resource), notificationDAO.find(notification), anyTypeKind, entityKey,
                page, size, orderByClauses).stream().
                <T>map(task -> binder.getTaskTO(task, taskUtilsFactory.getInstance(type), details)).
                collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_READ + "')")
    public <T extends AbstractTaskTO> T read(final String key, final boolean details) {
        Task task = taskDAO.find(key);
        if (task == null) {
            throw new NotFoundException("Task " + key);
        }
        return binder.getTaskTO(task, taskUtilsFactory.getInstance(task), details);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    @Override
    public ExecTO execute(final String key, final Date startAt, final boolean dryRun) {
        Task task = taskDAO.find(key);
        if (task == null) {
            throw new NotFoundException("Task " + key);
        }
        if (startAt != null && startAt.before(new Date())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add("Cannot schedule in the past");
            throw sce;
        }

        ExecTO result = null;
        switch (taskUtilsFactory.getInstance(task).getType()) {
            case PROPAGATION:
                TaskExec propExec = taskExecutor.execute((PropagationTask) task);
                result = binder.getExecTO(propExec);
                break;

            case NOTIFICATION:
                TaskExec notExec = notificationJobDelegate.executeSingle((NotificationTask) task);
                result = binder.getExecTO(notExec);
                break;

            case SCHEDULED:
            case PULL:
            case PUSH:
                if (!((SchedTask) task).isActive()) {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add("Task " + key + " is not active");
                    throw sce;
                }

                try {
                    Map<String, Object> jobDataMap = jobManager.register(
                            (SchedTask) task,
                            startAt,
                            confDAO.find("tasks.interruptMaxRetries", 1L));

                    jobDataMap.put(TaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);

                    if (startAt == null) {
                        scheduler.getScheduler().triggerJob(
                                JobNamer.getJobKey(task),
                                new JobDataMap(jobDataMap));
                    }
                } catch (Exception e) {
                    LOG.error("While executing task {}", task, e);

                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add(e.getMessage());
                    throw sce;
                }

                result = new ExecTO();
                result.setJobType(JobType.TASK);
                result.setRefKey(task.getKey());
                result.setRefDesc(binder.buildRefDesc(task));
                result.setStart(new Date());
                result.setStatus("JOB_FIRED");
                result.setMessage("Job fired; waiting for results...");
                break;

            default:
        }

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_DELETE + "')")
    public <T extends AbstractTaskTO> T delete(final String key) {
        Task task = taskDAO.find(key);
        if (task == null) {
            throw new NotFoundException("Task " + key);
        }
        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        T taskToDelete = binder.getTaskTO(task, taskUtils, true);

        if (TaskType.SCHEDULED == taskUtils.getType()
                || TaskType.PULL == taskUtils.getType()
                || TaskType.PUSH == taskUtils.getType()) {

            jobManager.unregister(task);
        }

        taskDAO.delete(task);
        return taskToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_READ + "')")
    @Override
    public int countExecutions(final String key) {
        return taskExecDAO.count(key);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_READ + "')")
    @Override
    public List<ExecTO> listExecutions(
            final String key, final int page, final int size, final List<OrderByClause> orderByClauses) {

        Task task = taskDAO.find(key);
        if (task == null) {
            throw new NotFoundException("Task " + key);
        }

        return taskExecDAO.findAll(task, page, size, orderByClauses).stream().
                map(taskExec -> binder.getExecTO(taskExec)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_LIST + "')")
    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return taskExecDAO.findRecent(max).stream().
                map(taskExec -> binder.getExecTO(taskExec)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_DELETE + "')")
    @Override
    public ExecTO deleteExecution(final String execKey) {
        TaskExec taskExec = taskExecDAO.find(execKey);
        if (taskExec == null) {
            throw new NotFoundException("Task execution " + execKey);
        }

        ExecTO taskExecutionToDelete = binder.getExecTO(taskExec);
        taskExecDAO.delete(taskExec);
        return taskExecutionToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_DELETE + "')")
    @Override
    public BulkActionResult deleteExecutions(
            final String key,
            final Date startedBefore, final Date startedAfter, final Date endedBefore, final Date endedAfter) {

        Task task = taskDAO.find(key);
        if (task == null) {
            throw new NotFoundException("Task " + key);
        }

        BulkActionResult result = new BulkActionResult();

        taskExecDAO.findAll(task, startedBefore, startedAfter, endedBefore, endedAfter).forEach(exec -> {
            try {
                taskExecDAO.delete(exec);
                result.getResults().put(String.valueOf(exec.getKey()), BulkActionResult.Status.SUCCESS);
            } catch (Exception e) {
                LOG.error("Error deleting execution {} of task {}", exec.getKey(), key, e);
                result.getResults().put(String.valueOf(exec.getKey()), BulkActionResult.Status.FAILURE);
            }
        });

        return result;
    }

    @Override
    protected Triple<JobType, String, String> getReference(final JobKey jobKey) {
        String key = JobNamer.getTaskKeyFromJobName(jobKey.getName());

        Task task = taskDAO.find(key);
        return task == null || !(task instanceof SchedTask)
                ? null
                : Triple.of(JobType.TASK, key, binder.buildRefDesc(task));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_LIST + "')")
    @Override
    public List<JobTO> listJobs() {
        return super.doListJobs();
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    @Override
    public void actionJob(final String key, final JobAction action) {
        Task task = taskDAO.find(key);
        if (task == null) {
            throw new NotFoundException("Task " + key);
        }

        doActionJob(JobNamer.getJobKey(task), action);
    }

    @Override
    protected AbstractTaskTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)
                && !"deleteExecution".equals(method.getName()) && !"readExecution".equals(method.getName())) {

            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AbstractTaskTO) {
                    key = ((AbstractTaskTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
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
