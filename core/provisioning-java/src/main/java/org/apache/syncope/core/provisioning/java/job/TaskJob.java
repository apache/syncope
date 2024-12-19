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

import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskJob extends Job {

    private static final Logger LOG = LoggerFactory.getLogger(TaskJob.class);

    /**
     * Task execution status.
     */
    public enum Status {

        SUCCESS,
        FAILURE

    }

    @Autowired
    private DomainHolder<?> domainHolder;

    @Autowired
    protected ImplementationDAO implementationDAO;

    private SchedTaskJobDelegate delegate;

    public SchedTaskJobDelegate getDelegate() {
        return delegate;
    }

    protected void delegate(final JobExecutionContext context, final String taskKey)
            throws ClassNotFoundException, JobExecutionException {

        String implKey = (String) context.getData().get(JobManager.DELEGATE_IMPLEMENTATION);
        Implementation impl = implementationDAO.findById(implKey).orElse(null);
        if (impl == null) {
            LOG.error("Could not find Implementation '{}', aborting", implKey);
        } else {
            delegate = ImplementationManager.build(impl);
            delegate.execute(
                    (TaskType) context.getData().get(JobManager.TASK_TYPE),
                    taskKey,
                    context);
        }
    }

    @Override
    protected void execute(final JobExecutionContext context) throws JobExecutionException {
        if (!domainHolder.getDomains().containsKey(context.getDomain())) {
            LOG.debug("Domain {} not found, skipping", context.getDomain());
            return;
        }

        String taskKey = (String) context.getData().get(JobManager.TASK_KEY);
        try {
            AuthContextUtils.runAsAdmin(context.getDomain(), () -> {
                try {
                    delegate(context, taskKey);
                } catch (Exception e) {
                    if (e instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            LOG.error("While executing task {}", taskKey, e);
            if (e.getCause() instanceof JobExecutionException jee) {
                throw jee;
            }
            throw new JobExecutionException("While executing task " + taskKey, e);
        }
    }
}
