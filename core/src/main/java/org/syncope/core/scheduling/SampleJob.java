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

import org.quartz.JobExecutionException;
import org.syncope.core.persistence.beans.SchedTask;

/**
 * Sample implementation for execution a scheduled task.
 *
 * @see SchedTask
 */
public class SampleJob extends AbstractJob {

    @Override
    protected String doExecute()
            throws JobExecutionException {

        if (!(task instanceof SchedTask)) {
            throw new JobExecutionException("Task " + taskId
                    + " isn't a SchedTask");
        }
        final SchedTask schedTask = (SchedTask) this.task;

        LOG.info("SampleJob running [SchedTask {}]", schedTask.getId());

        return "RUNNING";
    }
}
