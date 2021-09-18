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

import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.JobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.spring.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskJob extends AbstractInterruptableJob {

    private static final Logger LOG = LoggerFactory.getLogger(TaskJob.class);

    public static final String DRY_RUN_JOBDETAIL_KEY = "dryRun";

    public static final String DELEGATE_IMPLEMENTATION = "delegateImpl";

    /**
     * Task execution status.
     */
    public enum Status {

        SUCCESS,
        FAILURE

    }

    @Autowired
    private DomainHolder domainHolder;

    /**
     * Key, set by the caller, for identifying the task to be executed.
     */
    private String taskKey;

    private SchedTaskJobDelegate delegate;

    /**
     * Task key setter.
     *
     * @param taskKey to be set
     */
    public void setTaskKey(final String taskKey) {
        this.taskKey = taskKey;
    }

    @Override
    public JobDelegate getDelegate() {
        return delegate;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            String domain = context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY);
            if (domainHolder.getDomains().containsKey(domain)) {
                AuthContextUtils.callAsAdmin(domain, () -> {
                    try {
                        ImplementationDAO implementationDAO =
                                ApplicationContextProvider.getApplicationContext().getBean(ImplementationDAO.class);
                        Implementation implementation = implementationDAO.find(
                                context.getMergedJobDataMap().getString(DELEGATE_IMPLEMENTATION));
                        if (implementation == null) {
                            LOG.error("Could not find Implementation '{}', aborting",
                                    context.getMergedJobDataMap().getString(DELEGATE_IMPLEMENTATION));
                        } else {
                            delegate = ImplementationManager.build(implementation);
                            delegate.execute(
                                    taskKey,
                                    context.getMergedJobDataMap().getBoolean(DRY_RUN_JOBDETAIL_KEY),
                                    context);
                        }
                    } catch (Exception e) {
                        LOG.error("While executing task {}", taskKey, e);
                        throw new RuntimeException(e);
                    }

                    return null;
                });
            } else {
                LOG.debug("Domain {} not found, skipping", domain);
            }
        } catch (RuntimeException e) {
            LOG.error("While executing task {}", taskKey, e);
            throw new JobExecutionException("While executing task " + taskKey, e);
        }
    }
}
