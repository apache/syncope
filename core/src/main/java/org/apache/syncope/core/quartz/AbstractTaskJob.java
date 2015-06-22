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
package org.apache.syncope.core.quartz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.util.ExceptionUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract job implementation that delegates to concrete implementation the actual job execution and provides some
 * base features.
 * <strong>Extending this class will not provide support transaction management.</strong><br/>
 * Extend <tt>AbstractTransactionalTaskJob</tt> for this purpose.
 *
 * @see AbstractTransactionalTaskJob
 */
@DisallowConcurrentExecution
public abstract class AbstractTaskJob implements TaskJob {

    public static final String DRY_RUN_JOBDETAIL_KEY = "dryRun";

    /**
     * Task execution status.
     */
    public enum Status {

        SUCCESS,
        FAILURE

    }

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTaskJob.class);

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

    /**
     * Task execution DAO.
     */
    @Autowired
    private TaskExecDAO taskExecDAO;

    /**
     * Configuration DAO.
     */
    @Autowired
    private ConfDAO confDAO;

    /**
     * Notification manager.
     */
    @Autowired
    private NotificationManager notificationManager;

    /**
     * Audit manager.
     */
    @Autowired
    private AuditManager auditManager;

    /**
     * Id, set by the caller, for identifying the task to be executed.
     */
    protected Long taskId;

    /**
     * The actual task to be executed.
     */
    protected Task task;

    /**
     * The current running thread containing the task to be executed.
     */
    protected AtomicReference<Thread> runningThread = new AtomicReference<Thread>();

    /**
     * Task id setter.
     *
     * @param taskId to be set
     */
    @Override
    public void setTaskId(final Long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        this.runningThread.set(Thread.currentThread());
        task = taskDAO.find(taskId);
        if (task == null) {
            throw new JobExecutionException("Task " + taskId + " not found");
        }

        TaskExec execution = new TaskExec();
        execution.setStartDate(new Date());
        execution.setTask(task);

        Result result;

        try {
            execution.setMessage(doExecute(context.getMergedJobDataMap().getBoolean(DRY_RUN_JOBDETAIL_KEY)));
            execution.setStatus(Status.SUCCESS.name());
            result = Result.SUCCESS;
        } catch (JobExecutionException e) {
            LOG.error("While executing task " + taskId, e);
            result = Result.FAILURE;

            execution.setMessage(ExceptionUtil.getFullStackTrace(e));
            execution.setStatus(Status.FAILURE.name());
        }
        execution.setEndDate(new Date());

        if (hasToBeRegistered(execution)) {
            taskExecDAO.saveAndAdd(taskId, execution);
        }
        task = taskDAO.save(task);

        notificationManager.createTasks(
                AuditElements.EventCategoryType.TASK,
                this.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(), // searching for before object is too much expensive ...
                result,
                task,
                execution);

        auditManager.audit(
                AuditElements.EventCategoryType.TASK,
                task.getClass().getSimpleName(),
                null,
                null, // searching for before object is too much expensive ...
                result,
                task,
                (Object[]) null);
    }

    /**
     * The actual execution, delegated to child classes.
     *
     * @param dryRun whether to actually touch the data
     * @return the task execution status to be set
     * @throws JobExecutionException if anything goes wrong
     */
    protected abstract String doExecute(boolean dryRun) throws JobExecutionException;

    /**
     * Template method to determine whether this job's task execution has to be persisted or not.
     *
     * @param execution task execution
     * @return wether to persist or not
     */
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return false;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        Thread thread = this.runningThread.getAndSet(null);
        if (thread != null) {
            LOG.info("Interrupting job time {} ", (new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN, Locale.
                    getDefault())).format(new Date()));
            thread.interrupt();
            if (thread.isAlive()) {                
                long maxRetry = confDAO.find("tasks.interruptMaxRetries", "0").getValues().get(0).getLongValue();
                for (int i = 0; i <= maxRetry && thread.isAlive(); i++) {
                    thread.interrupt();
                }
                //if the thread is still alive, it should be available in the next stop
                if(thread.isAlive()) this.runningThread.set(thread);
            }
        } else {
            LOG.warn("Unable to retrieve the right thread related to the current job execution");
        }
    }
}
