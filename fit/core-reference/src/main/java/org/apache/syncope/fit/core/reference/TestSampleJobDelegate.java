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

import java.time.OffsetDateTime;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.java.job.AbstractSchedTaskJobDelegate;

/**
 * Sample implementation for executing a scheduled task.
 */
public class TestSampleJobDelegate extends AbstractSchedTaskJobDelegate<SchedTask> {

    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
        for (int i = 0; i < 2; i++) {
            LOG.debug("TestSampleJob#doExecute round {} time {}", i, OffsetDateTime.now());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new JobExecutionException("Job interrupted");
            }
        }

        LOG.info("TestSampleJob {} running [SchedTask {}]", (context.isDryRun()
                ? "dry "
                : ""), task.getKey());

        return (context.isDryRun()
                ? "DRY "
                : "") + "RUNNING";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return true;
    }
}
