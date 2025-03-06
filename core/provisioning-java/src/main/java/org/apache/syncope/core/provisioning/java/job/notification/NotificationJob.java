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
package org.apache.syncope.core.provisioning.java.job.notification;

import java.util.Optional;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.java.job.Job;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically checks for notification to send.
 *
 * @see org.apache.syncope.core.persistence.api.entity.task.NotificationTask
 */
public class NotificationJob extends Job {

    public enum Status {

        SENT,
        NOT_SENT

    }

    protected static final Logger LOG = LoggerFactory.getLogger(NotificationJob.class);

    public static final String DEFAULT_CRON_EXP = "0 0/5 * * * ?";

    protected final SecurityProperties securityProperties;

    protected final DomainHolder<?> domainHolder;

    protected final NotificationJobDelegate delegate;

    public NotificationJob(
            final SecurityProperties securityProperties,
            final DomainHolder<?> domainHolder,
            final NotificationJobDelegate delegate) {

        this.securityProperties = securityProperties;
        this.domainHolder = domainHolder;
        this.delegate = delegate;
    }

    @Override
    protected void execute(final JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Waking up...");
        String executor = Optional.ofNullable(context.getExecutor()).orElseGet(securityProperties::getAdminUser);
        for (String domain : domainHolder.getDomains().keySet()) {
            try {
                AuthContextUtils.runAsAdmin(domain, () -> {
                    try {
                        delegate.execute(executor);
                    } catch (Exception e) {
                        LOG.error("While sending out notifications", e);
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException e) {
                LOG.error("While sending out notifications", e);
                throw new JobExecutionException("While sending out notifications", e);
            }
        }

        LOG.debug("Sleeping again...");
    }
}
