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
package org.apache.syncope.fit.core.reference;

import java.util.Date;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.java.job.AbstractSchedTaskJobDelegate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Sample implementation for executing a scheduled task.
 */
public class TestSampleJobDelegate extends AbstractSchedTaskJobDelegate {

    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

        for (int i = 0; i < 2; i++) {
            LOG.debug("TestSampleJob#doExecute round {} time {}", i, new Date().toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new JobExecutionException("Job interrupted");
            }
        }

        LOG.info("TestSampleJob {} running [SchedTask {}]", (dryRun
                ? "dry "
                : ""), task.getKey());

        return (dryRun
                ? "DRY "
                : "") + "RUNNING";
    }

    @Override
    public void interrupt() {
    }

    @Override
    public boolean isInterrupted() {
        return false;
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }
}
