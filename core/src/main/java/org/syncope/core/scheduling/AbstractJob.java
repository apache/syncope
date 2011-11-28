/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.scheduling;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;

/**
 * Base job implementation that delegates to concrete implementation the actual
 * job execution and provides some background settings (like as the
 * corresponding Task, for example).
 */
public abstract class AbstractJob implements StatefulJob {

    public static final String DRY_RUN_JOBDETAIL_KEY = "dryRun";

    /**
     * Task execution status.
     */
    protected enum Status {

        SUCCESS,
        FAILURE

    }

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractJob.class);

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
     * Id, set by the caller, for identifying the task to be executed.
     */
    protected Long taskId;

    /**
     * The actual task to be executed.
     */
    protected Task task;

    /**
     * Task id setter.
     *
     * @param taskId to be set
     */
    public void setTaskId(final Long taskId) {
        this.taskId = taskId;
    }

    @Override
    public final void execute(final JobExecutionContext context)
            throws JobExecutionException {

        task = taskDAO.find(taskId);
        if (task == null) {
            throw new JobExecutionException("Task " + taskId + " not found");
        }

        TaskExec execution = new TaskExec();
        execution.setStartDate(new Date());
        execution.setTask(task);

        try {
            execution.setMessage(doExecute(
                    context.getMergedJobDataMap().
                    getBoolean(DRY_RUN_JOBDETAIL_KEY)));

            execution.setStatus(Status.SUCCESS.name());
        } catch (JobExecutionException e) {
            LOG.error("While executing task " + taskId, e);

            StringWriter exceptionWriter = new StringWriter();
            exceptionWriter.write(e.getMessage() + "\n\n");
            e.printStackTrace(new PrintWriter(exceptionWriter));
            execution.setMessage(exceptionWriter.toString());

            execution.setStatus(Status.FAILURE.name());
        }
        execution.setEndDate(new Date());
        if (hasToBeRegistered(execution)) {
            taskExecDAO.save(execution);
        }
    }

    /**
     * The actual execution, delegated to child classes.
     * 
     * @param dryRun whether to actually touch the data
     * @return the task execution status to be set
     * @throws JobExecutionException if anything goes wrong
     */
    protected abstract String doExecute(boolean dryRun)
            throws JobExecutionException;

    /**
     * Template method to determine whether this job's task execution has
     * to be persisted or not.
     * @param execution task execution
     * @return wether to persist or not
     */
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return false;
    }
}
