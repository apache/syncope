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

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import javax.ws.rs.core.Response;
import javax.xml.transform.stream.StreamSource;
import org.apache.cocoon.pipeline.NonCachingPipeline;
import org.apache.cocoon.pipeline.Pipeline;
import org.apache.cocoon.sax.SAXPipelineComponent;
import org.apache.cocoon.sax.component.XMLGenerator;
import org.apache.cocoon.sax.component.XMLSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.core.logic.cocoon.FopSerializer;
import org.apache.syncope.core.logic.cocoon.TextSerializer;
import org.apache.syncope.core.logic.cocoon.XSLTTransformer;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.xmlgraphics.util.MimeConstants;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ReportLogic extends AbstractExecutableLogic<ReportTO> {

    protected final ReportDAO reportDAO;

    protected final ReportExecDAO reportExecDAO;

    protected final ConfParamOps confParamOps;

    protected final ReportDataBinder binder;

    protected final EntityFactory entityFactory;

    public ReportLogic(
            final JobManager jobManager,
            final SchedulerFactoryBean scheduler,
            final ReportDAO reportDAO,
            final ReportExecDAO reportExecDAO,
            final ConfParamOps confParamOps,
            final ReportDataBinder binder,
            final EntityFactory entityFactory) {

        super(jobManager, scheduler);

        this.reportDAO = reportDAO;
        this.reportExecDAO = reportExecDAO;
        this.confParamOps = confParamOps;
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
                    confParamOps.get(AuthContextUtils.getDomain(), "tasks.interruptMaxRetries", 1L, Long.class),
                    AuthContextUtils.getUsername());
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_UPDATE + "')")
    public ReportTO update(final ReportTO reportTO) {
        Report report = reportDAO.find(reportTO.getKey());
        if (report == null) {
            throw new NotFoundException("Report " + reportTO.getKey());
        }

        binder.getReport(report, reportTO);
        report = reportDAO.save(report);
        try {
            jobManager.register(
                    report,
                    null,
                    confParamOps.get(AuthContextUtils.getDomain(), "tasks.interruptMaxRetries", 1L, Long.class),
                    AuthContextUtils.getUsername());
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_LIST + "')")
    @Transactional(readOnly = true)
    public List<ReportTO> list() {
        return reportDAO.findAll().stream().map(binder::getReportTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Transactional(readOnly = true)
    public ReportTO read(final String key) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_EXECUTE + "')")
    @Override
    public ExecTO execute(final String key, final OffsetDateTime startAt, final boolean dryRun) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        if (!report.isActive()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add("Report " + key + " is not active");
            throw sce;
        }

        try {
            jobManager.register(
                    report,
                    startAt,
                    confParamOps.get(AuthContextUtils.getDomain(), "tasks.interruptMaxRetries", 1L, Long.class),
                    AuthContextUtils.getUsername());

            scheduler.getScheduler().triggerJob(JobNamer.getJobKey(report));
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
        result.setStatus(ReportExecStatus.STARTED.name());
        result.setMessage("Job fired; waiting for results...");
        result.setExecutor(AuthContextUtils.getUsername());
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    public ReportExec getReportExec(final String executionKey) {
        ReportExec reportExec = reportExecDAO.find(executionKey);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionKey);
        }
        if (!ReportExecStatus.SUCCESS.name().equals(reportExec.getStatus()) || reportExec.getExecResult() == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidReportExec);
            sce.getElements().add(reportExec.getExecResult() == null
                    ? "No report data produced"
                    : "Report did not run successfully");
            throw sce;
        }
        return reportExec;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    public static void exportExecutionResult(final OutputStream os, final ReportExec reportExec,
            final ReportExecExportFormat format) {

        // streaming SAX handler from a compressed byte array stream
        try (ByteArrayInputStream bais = new ByteArrayInputStream(reportExec.getExecResult());
                ZipInputStream zis = new ZipInputStream(bais)) {

            // a single ZipEntry in the ZipInputStream (see ReportJob)
            zis.getNextEntry();

            Pipeline<SAXPipelineComponent> pipeline = new NonCachingPipeline<>();
            pipeline.addComponent(new XMLGenerator(zis));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("status", reportExec.getStatus());
            parameters.put("message", reportExec.getMessage());
            parameters.put("start", reportExec.getStart());
            parameters.put("end", reportExec.getEnd());

            switch (format) {
                case HTML:
                    XSLTTransformer xsl2html = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getHTMLTemplate(),
                                    StandardCharsets.UTF_8)));
                    xsl2html.setParameters(parameters);
                    pipeline.addComponent(xsl2html);
                    pipeline.addComponent(XMLSerializer.createXHTMLSerializer());
                    break;

                case PDF:
                    XSLTTransformer xsl2pdf = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getFOTemplate(),
                                    StandardCharsets.UTF_8)));
                    xsl2pdf.setParameters(parameters);
                    pipeline.addComponent(xsl2pdf);
                    pipeline.addComponent(new FopSerializer(MimeConstants.MIME_PDF));
                    break;

                case RTF:
                    XSLTTransformer xsl2rtf = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getFOTemplate(),
                                    StandardCharsets.UTF_8)));
                    xsl2rtf.setParameters(parameters);
                    pipeline.addComponent(xsl2rtf);
                    pipeline.addComponent(new FopSerializer(MimeConstants.MIME_RTF));
                    break;

                case CSV:
                    XSLTTransformer xsl2csv = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getCSVTemplate(),
                                    StandardCharsets.UTF_8)));
                    xsl2csv.setParameters(parameters);
                    pipeline.addComponent(xsl2csv);
                    pipeline.addComponent(new TextSerializer());
                    break;

                case XML:
                default:
                    pipeline.addComponent(XMLSerializer.createXMLSerializer());
            }

            pipeline.setup(os);
            pipeline.execute();

            LOG.debug("Result of {} successfully exported as {}", reportExec, format);
        } catch (Exception e) {
            LOG.error("While exporting content", e);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_DELETE + "')")
    public ReportTO delete(final String key) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        ReportTO deletedReport = binder.getReportTO(report);
        jobManager.unregister(report);
        reportDAO.delete(report);
        return deletedReport;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Override
    public Pair<Integer, List<ExecTO>> listExecutions(
            final String key, final int page, final int size, final List<OrderByClause> orderByClauses) {

        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        Integer count = reportExecDAO.count(key);

        List<ExecTO> result = reportExecDAO.findAll(report, page, size, orderByClauses).stream().
                map(reportExec -> binder.getExecTO(reportExec)).collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_LIST + "')")
    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return reportExecDAO.findRecent(max).stream().
                map(reportExec -> binder.getExecTO(reportExec)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_DELETE + "')")
    @Override
    public ExecTO deleteExecution(final String executionKey) {
        ReportExec reportExec = reportExecDAO.find(executionKey);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionKey);
        }

        ExecTO reportExecToDelete = binder.getExecTO(reportExec);
        reportExecDAO.delete(reportExec);
        return reportExecToDelete;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_DELETE + "')")
    @Override
    public List<BatchResponseItem> deleteExecutions(
            final String key,
            final OffsetDateTime startedBefore,
            final OffsetDateTime startedAfter,
            final OffsetDateTime endedBefore,
            final OffsetDateTime endedAfter) {

        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        List<BatchResponseItem> batchResponseItems = new ArrayList<>();

        reportExecDAO.findAll(report, startedBefore, startedAfter, endedBefore, endedAfter).forEach(exec -> {
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
    protected Triple<JobType, String, String> getReference(final JobKey jobKey) {
        String key = JobNamer.getReportKeyFromJobName(jobKey.getName());

        Report report = reportDAO.find(key);
        return Optional.ofNullable(report)
                .map(report1 -> Triple.of(JobType.REPORT, key, binder.buildRefDesc(report1))).orElse(null);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_LIST + "')")
    @Override
    public List<JobTO> listJobs() {
        return super.doListJobs(false);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    @Override
    public JobTO getJob(final String key) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        JobTO jobTO = null;
        try {
            jobTO = getJobTO(JobNamer.getJobKey(report), false);
        } catch (SchedulerException e) {
            LOG.error("Problems while retrieving scheduled job {}", JobNamer.getJobKey(report), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        if (jobTO == null) {
            throw new NotFoundException("Job for report " + key);
        }
        return jobTO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_EXECUTE + "')")
    @Override
    public void actionJob(final String key, final JobAction action) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        doActionJob(JobNamer.getJobKey(report), action);
    }

    @Override
    protected ReportTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args) && ("create".equals(method.getName())
                || "update".equals(method.getName())
                || "delete".equals(method.getName()))) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ReportTO) {
                    key = ((ReportTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getReportTO(reportDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
