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
package org.apache.syncope.core.logic;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ReportLogic extends AbstractExecutableLogic<ReportTO> {

    protected final ReportDAO reportDAO;

    protected final ReportExecDAO reportExecDAO;

    protected final ReportDataBinder binder;

    protected final EntityFactory entityFactory;

    public ReportLogic(
            final JobManager jobManager,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO,
            final ReportDAO reportDAO,
            final ReportExecDAO reportExecDAO,
            final ReportDataBinder binder,
            final EntityFactory entityFactory) {

        super(jobManager, scheduler, jobStatusDAO);

        this.reportDAO = reportDAO;
        this.reportExecDAO = reportExecDAO;
        this.binder = binder;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_CREATE + "')")
    public ReportTO create(final ReportTO reportTO) {
        Report report = entityFactory.newEntity(Report.class);
        binder.getReport(report, reportTO);
        report = reportDAO.save(report);
        try {
            jobManager.register(
                    report,
                    null,
                    AuthContextUtils.getUsername(),
                    false);
        } catch (Exception e) {
            LOG.error("While registering job for report {}", report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_UPDATE + "')")
    public ReportTO update(final ReportTO reportTO) {
        Report report = reportDAO.findById(reportTO.getKey()).
                orElseThrow(() -> new NotFoundException("Report " + reportTO.getKey()));

        binder.getReport(report, reportTO);
        report = reportDAO.save(report);
        try {
            jobManager.register(
                    report,
                    null,
                    AuthContextUtils.getUsername(),
                    false);
        } catch (Exception e) {
            LOG.error("While registering job for report {}", report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_LIST + "')")
    @Transactional(readOnly = true)
    public List<ReportTO> list() {
        return reportDAO.findAll().stream().map(binder::getReportTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Transactional(readOnly = true)
    public ReportTO read(final String key) {
        Report report = reportDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Report " + key));
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_EXECUTE + "')")
    @Override
    public ExecTO execute(final ExecSpecs specs) {
        Report report = reportDAO.findById(specs.getKey()).
                orElseThrow(() -> new NotFoundException("Report " + specs.getKey()));

        if (!report.isActive()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add("Report " + specs.getKey() + " is not active");
            throw sce;
        }

        if (specs.getStartAt() != null && specs.getStartAt().isBefore(OffsetDateTime.now())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add("Cannot schedule in the past");
            throw sce;
        }

        try {
            jobManager.register(
                    report,
                    Optional.ofNullable(specs.getStartAt()).orElseGet(OffsetDateTime::now),
                    AuthContextUtils.getUsername(),
                    specs.getDryRun());
        } catch (Exception e) {
            LOG.error("While executing report {}", report, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        ExecTO result = new ExecTO();
        result.setJobType(JobType.REPORT);
        result.setRefKey(report.getKey());
        result.setRefDesc(binder.buildRefDesc(report));
        result.setStart(OffsetDateTime.now());
        result.setStatus(JobStatusDAO.JOB_FIRED_STATUS);
        result.setMessage("Job fired; waiting for results...");
        result.setExecutor(AuthContextUtils.getUsername());
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Transactional(readOnly = true)
    public String getFilename(final String executionKey) {
        ReportExec reportExec = reportExecDAO.findById(executionKey).
                orElseThrow(() -> new NotFoundException("ReportExec " + executionKey));

        return reportExec.getReport().getName()
                + "."
                + StringUtils.removeStart(reportExec.getReport().getFileExt(), ".");
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Transactional(readOnly = true)
    public void exportExecutionResult(
            final OutputStream os,
            final String executionKey) {

        ReportExec reportExec = reportExecDAO.findById(executionKey).
                orElseThrow(() -> new NotFoundException("ReportExec " + executionKey));

        if (reportExec.getExecResult() == null || !ReportJob.Status.SUCCESS.name().equals(reportExec.getStatus())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidReportExec);
            sce.getElements().add(reportExec.getExecResult() == null
                    ? "No report data produced"
                    : "Report did not run successfully");
            throw sce;
        }

        // streaming output from a compressed byte array stream
        try (ByteArrayInputStream bais = new ByteArrayInputStream(reportExec.getExecResult());
                ZipInputStream zis = new ZipInputStream(bais)) {

            // a single ZipEntry in the ZipInputStream
            zis.getNextEntry();

            zis.transferTo(os);
        } catch (Exception e) {
            LOG.error("While exporting content", e);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_DELETE + "')")
    public ReportTO delete(final String key) {
        Report report = reportDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Report " + key));

        ReportTO deletedReport = binder.getReportTO(report);
        jobManager.unregister(report);
        reportDAO.delete(report);
        return deletedReport;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Override
    public Page<ExecTO> listExecutions(
            final String key,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        Report report = reportDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Report " + key));

        long count = reportExecDAO.count(report, before, after);

        List<ExecTO> result = reportExecDAO.findAll(report, before, after, pageable).stream().
                map(binder::getExecTO).toList();

        return new SyncopePage<>(result, pageable, count);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_LIST + "')")
    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return reportExecDAO.findRecent(max).stream().
                map(binder::getExecTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_DELETE + "')")
    @Override
    public ExecTO deleteExecution(final String executionKey) {
        ReportExec reportExec = reportExecDAO.findById(executionKey).
                orElseThrow(() -> new NotFoundException("ReportExec " + executionKey));

        ExecTO reportExecToDelete = binder.getExecTO(reportExec);
        reportExecDAO.delete(reportExec);
        return reportExecToDelete;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_DELETE + "')")
    @Override
    public List<BatchResponseItem> deleteExecutions(
            final String key,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        Report report = reportDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Report " + key));

        List<BatchResponseItem> batchResponseItems = new ArrayList<>();

        reportExecDAO.findAll(report, before, after, Pageable.unpaged()).forEach(exec -> {
            BatchResponseItem item = new BatchResponseItem();
            item.getHeaders().put(RESTHeaders.RESOURCE_KEY, List.of(exec.getKey()));
            batchResponseItems.add(item);

            try {
                reportExecDAO.delete(exec);
                item.setStatus(Response.Status.OK.getStatusCode());
            } catch (Exception e) {
                LOG.error("Error deleting execution {} of report {}", exec.getKey(), key, e);
                item.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                item.setContent(ExceptionUtils2.getFullStackTrace(e));
            }
        });

        return batchResponseItems;
    }

    @Override
    protected Triple<JobType, String, String> getReference(final String jobName) {
        return JobNamer.getReportKeyFromJobName(jobName).
                flatMap(reportDAO::findById).
                map(r -> Triple.of(JobType.REPORT, r.getKey(), binder.buildRefDesc(r))).
                orElse(null);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_LIST + "')")
    @Override
    public List<JobTO> listJobs() {
        return super.doListJobs(false);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Override
    public JobTO getJob(final String key) {
        Report report = reportDAO.findById(key).orElseThrow(() -> new NotFoundException("Report " + key));

        return getJobTO(JobNamer.getJobName(report), false).
                orElseThrow(() -> new NotFoundException("Job for report " + key));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_EXECUTE + "')")
    @Override
    public void actionJob(final String key, final JobAction action) {
        Report report = reportDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Report " + key));

        doActionJob(JobNamer.getJobName(report), action);
    }

    @Override
    protected ReportTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args) && ("create".equals(method.getName())
                || "update".equals(method.getName())
                || "delete".equals(method.getName()))) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof ReportTO reportTO) {
                    key = reportTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getReportTO(reportDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
