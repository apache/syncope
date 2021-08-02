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
package org.apache.syncope.core.provisioning.java.job.report;

import java.util.Optional;
import org.apache.syncope.core.provisioning.api.job.JobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.provisioning.java.job.AbstractInterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job for executing a given report.
 */
public class ReportJob extends AbstractInterruptableJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReportJob.class);

    @Autowired
    private SecurityProperties securityProperties;

    /**
     * Key, set by the caller, for identifying the report to be executed.
     */
    private String reportKey;

    @Autowired
    private ReportJobDelegate delegate;

    /**
     * Report id setter.
     *
     * @param reportKey to be set
     */
    public void setReportKey(final String reportKey) {
        this.reportKey = reportKey;
    }

    @Override
    public JobDelegate getDelegate() {
        return delegate;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            String domainKey = context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY);
            String executor = Optional.ofNullable(context.getMergedJobDataMap().getString(JobManager.EXECUTOR_KEY)).
                    orElse(securityProperties.getAdminUser());

            AuthContextUtils.callAsAdmin(domainKey, () -> {
                try {
                    delegate.execute(reportKey, executor);
                } catch (Exception e) {
                    LOG.error("While executing report {}", reportKey, e);
                    throw new RuntimeException(e);
                }

                return null;
            });
        } catch (RuntimeException e) {
            LOG.error("While executing report {}", reportKey, e);
            throw new JobExecutionException("While executing report " + reportKey, e);
        }
    }
}
