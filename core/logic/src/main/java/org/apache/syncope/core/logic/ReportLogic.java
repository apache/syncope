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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.apache.cocoon.optional.pipeline.components.sax.fop.FopSerializer;
import org.apache.cocoon.pipeline.NonCachingPipeline;
import org.apache.cocoon.pipeline.Pipeline;
import org.apache.cocoon.sax.SAXPipelineComponent;
import org.apache.cocoon.sax.component.XMLGenerator;
import org.apache.cocoon.sax.component.XMLSerializer;
import org.apache.cocoon.sax.component.XSLTTransformer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.job.JobInstanceLoader;
import org.apache.syncope.core.logic.report.TextSerializer;
import org.apache.syncope.common.lib.to.AbstractExecTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.xmlgraphics.util.MimeConstants;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ReportLogic extends AbstractJobLogic<ReportTO> {

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private ReportDataBinder binder;

    @Autowired
    private EntityFactory entityFactory;

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_CREATE + "')")
    public ReportTO create(final ReportTO reportTO) {
        Report report = entityFactory.newEntity(Report.class);
        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report, null);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_UPDATE + "')")
    public ReportTO update(final ReportTO reportTO) {
        Report report = reportDAO.find(reportTO.getKey());
        if (report == null) {
            throw new NotFoundException("Report " + reportTO.getKey());
        }

        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report, null);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_LIST + "')")
    public List<ReportTO> list() {
        return CollectionUtils.collect(reportDAO.findAll(),
                new Transformer<Report, ReportTO>() {

            @Override
            public ReportTO transform(final Report input) {
                return binder.getReportTO(input);
            }
        }, new ArrayList<ReportTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_READ + "')")
    public ReportTO read(final Long key) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_READ + "')")
    public void exportExecutionResult(final OutputStream os, final ReportExec reportExec,
            final ReportExecExportFormat format) {

        // streaming SAX handler from a compressed byte array stream
        ByteArrayInputStream bais = new ByteArrayInputStream(reportExec.getExecResult());
        ZipInputStream zis = new ZipInputStream(bais);
        try {
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
                    XSLTTransformer xsl2html = new XSLTTransformer(getClass().getResource("/report/report2html.xsl"));
                    xsl2html.setParameters(parameters);
                    pipeline.addComponent(xsl2html);
                    pipeline.addComponent(XMLSerializer.createXHTMLSerializer());
                    break;

                case PDF:
                    XSLTTransformer xsl2pdf = new XSLTTransformer(getClass().getResource("/report/report2fo.xsl"));
                    xsl2pdf.setParameters(parameters);
                    pipeline.addComponent(xsl2pdf);
                    pipeline.addComponent(new FopSerializer(MimeConstants.MIME_PDF));
                    break;

                case RTF:
                    XSLTTransformer xsl2rtf = new XSLTTransformer(getClass().getResource("/report/report2fo.xsl"));
                    xsl2rtf.setParameters(parameters);
                    pipeline.addComponent(xsl2rtf);
                    pipeline.addComponent(new FopSerializer(MimeConstants.MIME_RTF));
                    break;

                case CSV:
                    XSLTTransformer xsl2csv = new XSLTTransformer(getClass().getResource("/report/report2csv.xsl"));
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
        } finally {
            IOUtils.closeQuietly(zis);
            IOUtils.closeQuietly(bais);
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_READ + "')")
    public ReportExec getAndCheckReportExec(final Long executionKey) {
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

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_EXECUTE + "')")
    public ReportExecTO execute(final Long key, final Date startAt) {
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
            jobInstanceLoader.registerJob(report, startAt);

            scheduler.getScheduler().triggerJob(new JobKey(JobNamer.getJobName(report), Scheduler.DEFAULT_GROUP));
        } catch (Exception e) {
            LOG.error("While executing report {}", report, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        ReportExecTO result = new ReportExecTO();
        result.setReport(key);
        result.setStart(new Date());
        result.setStatus(ReportExecStatus.STARTED.name());
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_DELETE + "')")
    public ReportTO delete(final Long key) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        ReportTO deletedReport = binder.getReportTO(report);
        jobInstanceLoader.unregisterJob(report);
        reportDAO.delete(report);
        return deletedReport;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_DELETE + "')")
    public ReportExecTO deleteExecution(final Long executionKey) {
        ReportExec reportExec = reportExecDAO.find(executionKey);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionKey);
        }

        ReportExecTO reportExecToDelete = binder.getReportExecTO(reportExec);
        reportExecDAO.delete(reportExec);
        return reportExecToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_DELETE + "')")
    public BulkActionResult deleteExecutions(
            final Long key,
            final Date startedBefore, final Date startedAfter, final Date endedBefore, final Date endedAfter) {

        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        BulkActionResult result = new BulkActionResult();

        for (ReportExec exec : reportExecDAO.findAll(report, startedBefore, startedAfter, endedBefore, endedAfter)) {
            try {
                reportExecDAO.delete(exec);
                result.getResults().put(String.valueOf(exec.getKey()), BulkActionResult.Status.SUCCESS);
            } catch (Exception e) {
                LOG.error("Error deleting execution {} of report {}", exec.getKey(), key, e);
                result.getResults().put(String.valueOf(exec.getKey()), BulkActionResult.Status.FAILURE);
            }
        }

        return result;
    }

    @Override
    protected Long getKeyFromJobName(final JobKey jobKey) {
        return JobNamer.getReportKeyFromJobName(jobKey.getName());
    }

    @Override
    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_LIST + "')")
    public <E extends AbstractExecTO> List<E> listJobs(final JobStatusType type, final Class<E> reference) {
        return super.listJobs(type, reference);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_EXECUTE + "')")
    public void actionJob(final Long key, final JobAction action) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }
        String jobName = JobNamer.getJobName(report);
        actionJob(jobName, action);
    }

    @Override
    protected ReportTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long key = null;

        if (ArrayUtils.isNotEmpty(args) && ("create".equals(method.getName())
                || "update".equals(method.getName())
                || "delete".equals(method.getName()))) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof ReportTO) {
                    key = ((ReportTO) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
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
