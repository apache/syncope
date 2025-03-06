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
import java.util.Optional;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractSchedTaskJobDelegate<T extends SchedTask> implements SchedTaskJobDelegate {

    protected static final Logger LOG = LoggerFactory.getLogger(SchedTaskJobDelegate.class);

    @Autowired
    protected SecurityProperties securityProperties;

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

    protected boolean manageOperationId;

    protected String executor;

    protected void setStatus(final String status) {
        publisher.publishEvent(new JobStatusEvent(
                this, AuthContextUtils.getDomain(), JobNamer.getJobName(task), status));
    }

    @SuppressWarnings("unchecked")
    protected void init(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context)
            throws JobExecutionException {

        this.taskType = taskType;
        task = (T) taskDAO.findById(taskType, taskKey).
                orElseThrow(() -> new JobExecutionException("Not found: " + taskType + " Task " + taskKey));

        if (!task.isActive()) {
            LOG.info("Task {} not active, aborting...", taskKey);
            return;
        }

        manageOperationId = Optional.ofNullable(MDC.get(Job.OPERATION_ID)).
                map(operationId -> false).
                orElseGet(() -> {
                    MDC.put(Job.OPERATION_ID, SecureRandomUtils.generateRandomUUID().toString());
                    return true;
                });

        executor = Optional.ofNullable(context.getExecutor()).orElseGet(() -> securityProperties.getAdminUser());
    }

    protected TaskExec<SchedTask> initExecution() {
        TaskExec<SchedTask> execution = taskUtilsFactory.getInstance(taskType).newTaskExec();
        execution.setStart(OffsetDateTime.now());
        execution.setTask(task);
        execution.setExecutor(executor);

        return execution;
    }

    protected void endExecution(
            final TaskExec<SchedTask> execution,
            final String message,
            final String status,
            final OpEvent.Outcome result) {

        execution.setMessage(message);
        execution.setStatus(status);
        execution.setEnd(OffsetDateTime.now());

        if (hasToBeRegistered(execution)) {
            register(execution);
        }
        task = taskDAO.save(task);

        notificationManager.createTasks(
                executor,
                OpEvent.CategoryType.TASK,
                this.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(),
                result,
                task,
                execution);

        auditManager.audit(
                AuthContextUtils.getDomain(),
                executor,
                OpEvent.CategoryType.TASK,
                task.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(),
                result,
                task,
                execution);
    }

    protected void end() {
        if (manageOperationId) {
            MDC.remove(Job.OPERATION_ID);
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public void execute(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context)
            throws JobExecutionException {

        init(taskType, taskKey, context);

        setStatus("Initialization completed");

        TaskExec<SchedTask> execution = initExecution();

        String message;
        String status;
        OpEvent.Outcome result;
        try {
            message = doExecute(context);
            status = TaskJob.Status.SUCCESS.name();
            result = OpEvent.Outcome.SUCCESS;
        } catch (JobExecutionException e) {
            LOG.error("While executing task {}", taskKey, e);

            message = ExceptionUtils2.getFullStackTrace(e);
            status = TaskJob.Status.FAILURE.name();
            result = OpEvent.Outcome.FAILURE;
        }
        endExecution(execution, message, status, result);

        end();
    }

    /**
     * The actual execution, delegated to child classes.
     *
     * @param context job execution context, can be used to pass parameters to the job
     * @return the task execution status to be set
     * @throws JobExecutionException if anything goes wrong
     */
    protected abstract String doExecute(JobExecutionContext context) throws JobExecutionException;

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
