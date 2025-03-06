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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractReportJobDelegate implements ReportJobDelegate {

    protected static final Logger LOG = LoggerFactory.getLogger(ReportJobDelegate.class);

    @Autowired
    protected SecurityProperties securityProperties;

    /**
     * The actual report to be executed.
     */
    protected Report report;

    protected ReportConf conf;

    /**
     * Report DAO.
     */
    @Autowired
    protected ReportDAO reportDAO;

    @Autowired
    protected EntityFactory entityFactory;

    /**
     * Notification manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit manager.
     */
    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected ApplicationEventPublisher publisher;

    @Override
    public void setConf(final ReportConf conf) {
        this.conf = conf;
    }

    protected void setStatus(final String status) {
        publisher.publishEvent(new JobStatusEvent(
                this, AuthContextUtils.getDomain(), JobNamer.getJobName(report), status));
    }

    @Transactional
    @Override
    public void execute(
            final String reportKey,
            final boolean dryRun,
            final JobExecutionContext context) throws JobExecutionException {

        report = reportDAO.findById(reportKey).
                orElseThrow(() -> new JobExecutionException("Report " + reportKey + " not found"));

        if (!report.isActive()) {
            LOG.info("Report {} not active, aborting...", reportKey);
            return;
        }

        String executor = Optional.ofNullable(context.getExecutor()).orElseGet(() -> securityProperties.getAdminUser());
        ReportExec execution = entityFactory.newEntity(ReportExec.class);
        execution.setStart(OffsetDateTime.now());
        execution.setReport(report);
        execution.setExecutor(executor);

        setStatus("Initialization completed");

        OpEvent.Outcome result;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        zos.setLevel(Deflater.BEST_COMPRESSION);
        try {
            // a single ZipEntry in the ZipOutputStream
            zos.putNextEntry(new ZipEntry(report.getName()));
        } catch (IOException e) {
            throw new JobExecutionException("While configuring for output", e);
        }

        setStatus("Starting");
        try {
            execution.setMessage(doExecute(dryRun, zos, executor, context));
            execution.setStatus(ReportJob.Status.SUCCESS.name());

            result = OpEvent.Outcome.SUCCESS;
        } catch (JobExecutionException e) {
            LOG.error("While executing report {}", reportKey, e);
            result = OpEvent.Outcome.FAILURE;

            execution.setMessage(ExceptionUtils2.getFullStackTrace(e));
            execution.setStatus(ReportJob.Status.FAILURE.name());
        } finally {
            try {
                zos.closeEntry();
                zos.close();
            } catch (IOException e) {
                LOG.error("While closing output", e);
            }
        }
        if (result == OpEvent.Outcome.SUCCESS) {
            execution.setExecResult(baos.toByteArray());
        }
        execution.setEnd(OffsetDateTime.now());

        report.add(execution);
        report = reportDAO.save(report);

        notificationManager.createTasks(
                executor,
                OpEvent.CategoryType.REPORT,
                this.getClass().getSimpleName(),
                null,
                this.getClass().getSimpleName(), // searching for before object is too much expensive ...
                result,
                report,
                execution);

        auditManager.audit(
                AuthContextUtils.getDomain(),
                executor,
                OpEvent.CategoryType.REPORT,
                report.getClass().getSimpleName(),
                null,
                null, // searching for before object is too much expensive ...
                result,
                report,
                null);
    }

    /**
     * The actual execution, delegated to child classes.
     *
     * @param dryRun whether to actually touch the data
     * @param os where to stream report execution's data
     * @param executor the user executing this report
     * @param context job execution context, can be used to pass parameters to the job
     * @return the report execution status to be set
     * @throws JobExecutionException if anything goes wrong
     */
    protected abstract String doExecute(
            boolean dryRun, OutputStream os, String executor, JobExecutionContext context)
            throws JobExecutionException;
}
