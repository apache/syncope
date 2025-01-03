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

import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class Job implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    public static final String OPERATION_ID = "operation.id";

    @Autowired
    private JobStatusDAO jobStatusDAO;

    private JobExecutionContext context;

    public JobExecutionContext getContext() {
        return context;
    }

    public void setContext(final JobExecutionContext context) {
        this.context = context;
    }

    protected abstract void execute(JobExecutionContext context) throws JobExecutionException;

    @Override
    public void run() {
        setContext(Optional.ofNullable(context).orElseGet(() -> new JobExecutionContext(
                AuthContextUtils.getDomain(),
                getClass().getName() + "_" + SecureRandomUtils.generateRandomUUID().toString(),
                AuthContextUtils.getWho(),
                false)));

        boolean locked = false;
        try {
            locked = AuthContextUtils.callAsAdmin(context.getDomain(), () -> jobStatusDAO.lock(context.getJobName()));
        } catch (Exception e) {
            LOG.debug("While attempting to lock job {}", context.getJobName(), e);
        }
        if (!locked) {
            LOG.debug("Could not lock job {}, skipping execution", context.getJobName());
            return;
        }

        LOG.debug("Job {} locked, starting execution", context.getJobName());

        try {
            execute(context);
        } catch (JobExecutionException e) {
            LOG.error("While executing job {}", context.getJobName(), e);
        } finally {
            LOG.debug("Job {} execution completed", context.getJobName());

            AuthContextUtils.runAsAdmin(context.getDomain(), () -> jobStatusDAO.unlock(context.getJobName()));
        }
    }
}
