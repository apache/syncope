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


import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public abstract class AbstractInterruptableJob implements InterruptableJob {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInterruptableJob.class);
    
    /**
     * The current running thread containing the task to be executed.
     */
    private final AtomicReference<Thread> runningThread = new AtomicReference<>();

    private long interruptMaxRetries = 1;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        this.runningThread.set(Thread.currentThread());
        this.interruptMaxRetries = context.getMergedJobDataMap().getLong(JobManager.INTERRUPT_MAX_RETRIES_KEY);
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        Thread thread = this.runningThread.getAndSet(null);
        if (thread == null) {
            LOG.warn("Unable to retrieve the thread of the current job execution");
        } else {
            LOG.info("Interrupting job from thread {} at {} ", thread.getId(), FormatUtils.format(new Date()));

            if (interruptMaxRetries < 1) {
                interruptMaxRetries = 1;
            }
            for (int i = 0; i < interruptMaxRetries && thread.isAlive(); i++) {
                thread.interrupt();
            }
            // if the thread is still alive, it should be available in the next stop
            if (thread.isAlive()) {
                this.runningThread.set(thread);
            }
        }
    }

}
