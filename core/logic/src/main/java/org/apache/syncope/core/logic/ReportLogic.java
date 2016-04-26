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
import javax.xml.transform.stream.StreamSource;
import org.apache.cocoon.optional.pipeline.components.sax.fop.FopSerializer;
import org.apache.cocoon.pipeline.NonCachingPipeline;
import org.apache.cocoon.pipeline.Pipeline;
import org.apache.cocoon.sax.SAXPipelineComponent;
import org.apache.cocoon.sax.component.XMLGenerator;
import org.apache.cocoon.sax.component.XMLSerializer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ExecTO;
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
import org.apache.syncope.core.logic.report.TextSerializer;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.logic.report.XSLTTransformer;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.xmlgraphics.util.MimeConstants;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ReportLogic extends AbstractJobLogic<ReportTO> {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

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
            jobManager.register(
                    report,
                    null,
                    confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).getLongValue());
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
            jobManager.register(
                    report,
                    null,
                    confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).getLongValue());
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
    public ReportTO read(final String key) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_EXECUTE + "')")
    public ExecTO execute(final String key, final Date startAt) {
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
                    confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).getLongValue());

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
        result.setStart(new Date());
        result.setStatus(ReportExecStatus.STARTED.name());
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_READ + "')")
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
                    XSLTTransformer xsl2html = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getHTMLTemplate(),
                                    SyncopeConstants.DEFAULT_CHARSET)));
                    xsl2html.setParameters(parameters);
                    pipeline.addComponent(xsl2html);
                    pipeline.addComponent(XMLSerializer.createXHTMLSerializer());
                    break;

                case PDF:
                    XSLTTransformer xsl2pdf = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getFOTemplate(),
                                    SyncopeConstants.DEFAULT_CHARSET)));
                    xsl2pdf.setParameters(parameters);
                    pipeline.addComponent(xsl2pdf);
                    pipeline.addComponent(new FopSerializer(MimeConstants.MIME_PDF));
                    break;

                case RTF:
                    XSLTTransformer xsl2rtf = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getFOTemplate(),
                                    SyncopeConstants.DEFAULT_CHARSET)));
                    xsl2rtf.setParameters(parameters);
                    pipeline.addComponent(xsl2rtf);
                    pipeline.addComponent(new FopSerializer(MimeConstants.MIME_RTF));
                    break;

                case CSV:
                    XSLTTransformer xsl2csv = new XSLTTransformer(new StreamSource(
                            IOUtils.toInputStream(reportExec.getReport().getTemplate().getCSVTemplate(),
                                    SyncopeConstants.DEFAULT_CHARSET)));
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

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_DELETE + "')")
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

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_LIST + "')")
    public List<ExecTO> listRecentExecutions(final int max) {
        return CollectionUtils.collect(reportExecDAO.findRecent(max), new Transformer<ReportExec, ExecTO>() {

            @Override
            public ExecTO transform(final ReportExec reportExec) {
                return binder.getExecTO(reportExec);
            }
        }, new ArrayList<ExecTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_DELETE + "')")
    public ExecTO deleteExecution(final String executionKey) {
        ReportExec reportExec = reportExecDAO.find(executionKey);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionKey);
        }

        ExecTO reportExecToDelete = binder.getExecTO(reportExec);
        reportExecDAO.delete(reportExec);
        return reportExecToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_DELETE + "')")
    public BulkActionResult deleteExecutions(
            final String key,
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
    protected Triple<JobType, String, String> getReference(final JobKey jobKey) {
        String key = JobNamer.getReportKeyFromJobName(jobKey.getName());

        Report report = reportDAO.find(key);
        return report == null
                ? null
                : Triple.of(JobType.REPORT, key, binder.buildRefDesc(report));
    }

    @Override
    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_LIST + "')")
    public List<JobTO> listJobs() {
        return super.listJobs();
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_EXECUTE + "')")
    public void actionJob(final String key, final JobAction action) {
        Report report = reportDAO.find(key);
        if (report == null) {
            throw new NotFoundException("Report " + key);
        }

        actionJob(JobNamer.getJobKey(report), action);
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
