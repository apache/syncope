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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.java.job.Job;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Job executing a given report.
 */
public class ReportJob extends Job {

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
    private DomainHolder<?> domainHolder;

    @Autowired
    protected ImplementationDAO implementationDAO;

    @Autowired
    protected AuthDataAccessor authDataAccessor;

    private ReportJobDelegate delegate;

    @Override
    public ReportJobDelegate getDelegate() {
        return delegate;
    }

    protected void delegate(final JobExecutionContext context, final String reportKey)
            throws ClassNotFoundException, JobExecutionException {

        String implKey = (String) context.data().get(JobManager.DELEGATE_IMPLEMENTATION);
        Implementation impl = implementationDAO.findById(implKey).orElse(null);
        if (impl == null) {
            LOG.error("Could not find Implementation '{}', aborting", implKey);
        } else {
            delegate = ImplementationManager.buildReportJobDelegate(
                    context.domain(),
                    impl,
                    () -> perContextReportJobDelegates.get(impl.getKey()),
                    instance -> perContextReportJobDelegates.put(impl.getKey(), instance)).
                    orElseThrow(() -> new IllegalArgumentException("Could not instantiate " + impl.getBody()));
            delegate.execute(reportKey, context);
        }
    }

    @Override
    protected void execute(final JobExecutionContext context) throws JobExecutionException {
        if (!domainHolder.getDomains().containsKey(context.domain())) {
            LOG.debug("Domain {} not found, skipping", context.domain());
            return;
        }

        String reportKey = (String) context.data().get(JobManager.REPORT_KEY);
        try {
            Set<SyncopeGrantedAuthority> authorities = AuthContextUtils.callAsAdmin(
                    context.domain(),
                    () -> authDataAccessor.getAuthorities(context.executor(), null));

            AuthContextUtils.callAs(context.domain(), context.executor(), authorities, () -> {
                try {
                    delegate(context, reportKey);
                    return null;
                } catch (Exception e) {
                    if (e instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            LOG.error("While executing report {}", reportKey, e);
            throw new JobExecutionException("While executing report " + reportKey, e);
        }
    }
}
