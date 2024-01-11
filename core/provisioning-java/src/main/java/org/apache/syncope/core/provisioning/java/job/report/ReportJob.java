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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.JobDelegate;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.java.job.AbstractInterruptableJob;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Quartz job for executing a given report.
 */
public class ReportJob extends AbstractInterruptableJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReportJob.class);

    /**
     * Report execution status.
     */
    public enum Status {

        SUCCESS,
        FAILURE

    }

    private final Map<String, ReportJobDelegate> perContextReportJobDelegates = new ConcurrentHashMap<>();

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private DomainHolder domainHolder;

    private ReportJobDelegate delegate;

    @Override
    public JobDelegate getDelegate() {
        return delegate;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        String domain = context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY);
        if (!domainHolder.getDomains().containsKey(domain)) {
            LOG.debug("Domain {} not found, skipping", domain);
            return;
        }

        String reportKey = context.getMergedJobDataMap().getString(JobManager.REPORT_KEY);
        try {
            AuthContextUtils.runAsAdmin(domain, () -> {
                try {
                    String implKey = context.getMergedJobDataMap().getString(JobManager.DELEGATE_IMPLEMENTATION);
                    Implementation impl = implementationDAO.findById(implKey).orElse(null);
                    if (impl == null) {
                        LOG.error("Could not find Implementation '{}', aborting", implKey);
                    } else {
                        delegate = ImplementationManager.buildReportJobDelegate(
                                impl,
                                () -> perContextReportJobDelegates.get(impl.getKey()),
                                instance -> perContextReportJobDelegates.put(impl.getKey(), instance)).
                                orElseThrow(() -> new IllegalArgumentException(
                                "Could not instantiate " + impl.getBody()));
                        delegate.execute(
                                reportKey,
                                context.getMergedJobDataMap().getBoolean(JobManager.DRY_RUN_JOBDETAIL_KEY),
                                context);
                    }
                } catch (Exception e) {
                    LOG.error("While executing report {}", reportKey, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            LOG.error("While executing report {}", reportKey, e);
            throw new JobExecutionException("While executing report " + reportKey, e);
        }
    }
}
