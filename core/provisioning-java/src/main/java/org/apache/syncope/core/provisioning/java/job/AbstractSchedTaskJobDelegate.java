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
package org.apache.syncope.core.provisioning.java.job;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractSchedTaskJobDelegate<T extends SchedTask> implements SchedTaskJobDelegate {

    protected static final Logger LOG = LoggerFactory.getLogger(SchedTaskJobDelegate.class);

    @Autowired
    private SecurityProperties securityProperties;

    protected TaskType taskType;

    /**
     * The actual task to be executed.
     */
    protected T task;

    /**
     * Task execution DAO.
     */
    @Autowired
    protected TaskExecDAO taskExecDAO;

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected TaskUtilsFactory taskUtilsFactory;

    @Autowired
    protected TaskDataBinder taskDataBinder;

    /**
     * Notification manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit manager.
     */
    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected ApplicationEventPublisher publisher;

    protected boolean interrupt;

    protected boolean interrupted;

    protected void setStatus(final String status) {
        Objects.requireNonNull(task, "Task cannot be undefined");
        publisher.publishEvent(new JobStatusEvent(this, taskDataBinder.buildRefDesc(task), status));
    }

    @Override
    public void interrupt() {
        interrupt = true;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public void execute(
            final TaskType taskType,
            final String taskKey,
            final boolean dryRun,
            final JobExecutionContext context)
            throws JobExecutionException {

        this.taskType = taskType;
        task = (T) taskDAO.find(taskType, taskKey);
        if (task == null) {
            throw new JobExecutionException("Task " + taskKey + " not found");
        }

        if (!task.isActive()) {
            LOG.info("Task {} not active, aborting...", taskKey);
            return;
        }

        String executor = Optional.ofNullable(context.getMergedJobDataMap().getString(JobManager.EXECUTOR_KEY)).
                orElse(securityProperties.getAdminUser());
        TaskExec<SchedTask> execution = taskUtilsFactory.getInstance(taskType).newTaskExec();
        execution.setStart(OffsetDateTime.now());
        execution.setTask(task);
        execution.setExecutor(executor);

        setStatus("Initialization completed");

        AuditElements.Result result;

        try {
            execution.setMessage(doExecute(dryRun, executor, context));
            execution.setStatus(TaskJob.Status.SUCCESS.name());
            result = AuditElements.Result.SUCCESS;
        } catch (JobExecutionException e) {
            LOG.error("While executing task {}", taskKey, e);
            result = AuditElements.Result.FAILURE;

            execution.setMessage(ExceptionUtils2.getFullStackTrace(e));
            execution.setStatus(TaskJob.Status.FAILURE.name());
        }
        execution.setEnd(OffsetDateTime.now());

        if (hasToBeRegistered(execution)) {
            register(execution);
        }
        task = (T) taskDAO.save(task);

        setStatus(null);

        notificationManager.createTasks(
                executor,
                AuditElements.EventCategoryType.TASK,
                this.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(), // searching for before object is too much expensive ...
                result,
                task,
                execution);

        auditManager.audit(
                executor,
                AuditElements.EventCategoryType.TASK,
                task.getClass().getSimpleName(),
                null,
                null, // searching for before object is too much expensive ...
                result,
                task,
                null);
    }

    /**
     * The actual execution, delegated to child classes.
     *
     * @param dryRun whether to actually touch the data
     * @param executor the user executing this task
     * @param context Quartz' execution context, can be used to pass parameters to the job
     * @return the task execution status to be set
     * @throws JobExecutionException if anything goes wrong
     */
    protected abstract String doExecute(boolean dryRun, String executor, JobExecutionContext context)
            throws JobExecutionException;

    /**
     * Template method to determine whether this job's task execution has to be persisted or not.
     *
     * @param execution task execution
     * @return whether to persist or not
     */
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return false;
    }

    protected void register(final TaskExec<?> execution) {
        taskExecDAO.saveAndAdd(taskType, task.getKey(), execution);
    }
}
