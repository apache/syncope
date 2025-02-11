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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;

/**
 * Job for asynchronous handling of notification / audit events.
 * Instead of direct synchronous invocation - which occurs in the same transaction where the event is generated, the
 * execution of the scheduled code happens in a new transaction.
 */
public class AfterHandlingJob extends Job {

    private static final Logger LOG = LoggerFactory.getLogger(AfterHandlingJob.class);

    public static void schedule(final SyncopeTaskScheduler scheduler, final Map<String, Object> jobMap) {
        JobExecutionContext context = new JobExecutionContext(
                AuthContextUtils.getDomain(),
                AfterHandlingJob.class.getSimpleName() + "_" + SecureRandomUtils.generateRandomUUID(),
                AuthContextUtils.getWho(),
                false);
        context.getData().putAll(jobMap);

        try {
            AfterHandlingJob job = ApplicationContextProvider.getBeanFactory().createBean(AfterHandlingJob.class);
            job.setContext(context);
            scheduler.schedule(job, Instant.now());
        } catch (TaskRejectedException e) {
            LOG.error("Could not schedule, aborting", e);
        }
    }

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Override
    protected void execute(final JobExecutionContext context) throws JobExecutionException {
        Optional<AfterHandlingEvent> event = Optional.ofNullable(
                context.getData().get(AfterHandlingEvent.JOBMAP_KEY)).map(AfterHandlingEvent.class::cast);
        if (event.isEmpty()) {
            LOG.debug("No event to process, aborting");
            return;
        }

        try {
            AuthContextUtils.runAsAdmin(context.getDomain(), () -> {
                notificationManager.createTasks(event.get());
                auditManager.audit(event.get());
            });
        } catch (RuntimeException e) {
            throw new JobExecutionException("While handling notification / audit events", e);
        }
    }
}
