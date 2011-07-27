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
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;

public abstract class AbstractJob implements Job {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractJob.class);

    protected static final String SUCCESS = "SUCCESS";

    protected static final String FAILURE = "FAILURE";

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    private Long taskId;

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public final void execute()
            throws JobExecutionException {

        final Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new JobExecutionException("Task " + taskId + " not found");
        }

        TaskExec execution = new TaskExec();
        execution.setStartDate(new Date());
        execution.setTask(task);

        String message;
        try {
            message = doExecute();

            execution.setStatus(SUCCESS);
        } catch (JobExecutionException e) {
            LOG.error("While executing task " + taskId, e);

            StringWriter exceptionWriter = new StringWriter();
            exceptionWriter.write(e.getMessage() + "\n\n");
            e.printStackTrace(new PrintWriter(exceptionWriter));
            message = exceptionWriter.toString();

            execution.setStatus(FAILURE);
        }
        execution.setMessage(message);
        execution.setEndDate(new Date());

        taskExecDAO.save(execution);
    }

    protected abstract String doExecute()
            throws JobExecutionException;
}
