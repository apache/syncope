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
package org.apache.syncope.core.rest.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import org.apache.cocoon.optional.pipeline.components.sax.fop.FopSerializer;
import org.apache.cocoon.pipeline.NonCachingPipeline;
import org.apache.cocoon.pipeline.Pipeline;
import org.apache.cocoon.sax.SAXPipelineComponent;
import org.apache.cocoon.sax.component.XMLGenerator;
import org.apache.cocoon.sax.component.XMLSerializer;
import org.apache.cocoon.sax.component.XSLTTransformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.report.ReportletConf;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;
import static org.apache.syncope.common.types.ReportExecExportFormat.RTF;
import org.apache.syncope.common.types.ReportExecStatus;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.init.JobInstanceLoader;
import org.apache.syncope.core.persistence.beans.Report;
import org.apache.syncope.core.persistence.beans.ReportExec;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.ReportDAO;
import org.apache.syncope.core.persistence.dao.ReportExecDAO;
import org.apache.syncope.core.report.Reportlet;
import org.apache.syncope.core.report.cocoon.TextSerializer;
import org.apache.syncope.core.rest.data.ReportDataBinder;
import org.apache.xmlgraphics.util.MimeConstants;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/report")
public class ReportController extends AbstractTransactionalController<ReportTO> {

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private ReportDataBinder binder;

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public ReportTO create(final HttpServletResponse response, @RequestBody final ReportTO reportTO) {
        ReportTO createdReportTO = createInternal(reportTO);
        response.setStatus(HttpServletResponse.SC_CREATED);
        return createdReportTO;
    }

    @PreAuthorize("hasRole('REPORT_CREATE')")
    public ReportTO createInternal(final ReportTO reportTO) {
        LOG.debug("Creating report " + reportTO);

        Report report = new Report();
        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getId(), e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('REPORT_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public ReportTO update(@RequestBody final ReportTO reportTO) {
        LOG.debug("Report update called with parameter {}", reportTO);

        Report report = reportDAO.find(reportTO.getId());
        if (report == null) {
            throw new NotFoundException("Report " + reportTO.getId());
        }

        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getId(), e);

            SyncopeClientCompositeErrorException sccee =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            sccee.addException(sce);
            throw sccee;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/count")
    public ModelAndView count() {
        return new ModelAndView().addObject(reportDAO.count());
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public List<ReportTO> list() {
        List<Report> reports = reportDAO.findAll();
        List<ReportTO> result = new ArrayList<ReportTO>(reports.size());
        for (Report report : reports) {
            result.add(binder.getReportTO(report));
        }
        return result;
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list/{page}/{size}")
    public List<ReportTO> list(@PathVariable("page") final int page, @PathVariable("size") final int size) {
        List<Report> reports = reportDAO.findAll(page, size);
        List<ReportTO> result = new ArrayList<ReportTO>(reports.size());
        for (Report report : reports) {
            result.add(binder.getReportTO(report));
        }
        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/reportletConfClasses")
    public ModelAndView getReportletConfClasses() {
        Set<String> reportletConfClasses = getReportletConfClassesInternal();
        return new ModelAndView().addObject(reportletConfClasses);
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @SuppressWarnings("rawtypes")
    public Set<String> getReportletConfClassesInternal() {
        Set<String> reportletConfClasses = new HashSet<String>();

        for (Class<Reportlet> reportletClass : binder.getAllReportletClasses()) {
            Class<? extends ReportletConf> reportletConfClass = binder.getReportletConfClass(reportletClass);
            if (reportletConfClass != null) {
                reportletConfClasses.add(reportletConfClass.getName());
            }
        }
        return reportletConfClasses;
    }

    @PreAuthorize("hasRole('REPORT_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{reportId}")
    public ReportTO read(@PathVariable("reportId") final Long reportId) {
        Report report = reportDAO.find(reportId);
        if (report == null) {
            throw new NotFoundException("Report " + reportId);
        }
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('REPORT_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/read/{executionId}")
    @Transactional(readOnly = true)
    public ReportExecTO readExecution(@PathVariable("executionId") final Long executionId) {
        ReportExec reportExec = reportExecDAO.find(executionId);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionId);
        }
        return binder.getReportExecTO(reportExec);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/execution/export/{executionId}")
    @Transactional(readOnly = true)
    public void exportExecutionResult(final HttpServletResponse response,
            @PathVariable("executionId") final Long executionId,
            @RequestParam(value = "fmt", required = false) final ReportExecExportFormat fmt) {

        OutputStream os;
        try {
            os = response.getOutputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Could not get output stream", e);
        }
        ReportExec reportExec = getAndCheckReportExecInternal(executionId);

        ReportExecExportFormat format = (fmt == null) ? ReportExecExportFormat.XML : fmt;

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        response.addHeader(SyncopeConstants.CONTENT_DISPOSITION_HEADER,
                "attachment; filename=" + reportExec.getReport().getName() + "." + format.name().toLowerCase());

        exportExecutionResultInternal(os, reportExec, format);
    }

    @PreAuthorize("hasRole('REPORT_READ')")
    public void exportExecutionResultInternal(final OutputStream os, final ReportExec reportExec,
            final ReportExecExportFormat format) {

        LOG.debug("Exporting result of {} as {}", reportExec, format);

        // streaming SAX handler from a compressed byte array stream
        ByteArrayInputStream bais = new ByteArrayInputStream(reportExec.getExecResult());
        ZipInputStream zis = new ZipInputStream(bais);
        try {
            // a single ZipEntry in the ZipInputStream (see ReportJob)
            zis.getNextEntry();

            Pipeline<SAXPipelineComponent> pipeline = new NonCachingPipeline<SAXPipelineComponent>();
            pipeline.addComponent(new XMLGenerator(zis));
            
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("status", reportExec.getStatus());
            parameters.put("message", reportExec.getMessage());
            parameters.put("startDate", reportExec.getStartDate());
            parameters.put("endDate", reportExec.getEndDate());

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

    @PreAuthorize("hasRole('REPORT_READ')")
    public ReportExec getAndCheckReportExecInternal(final Long executionId) {
        ReportExec reportExec = reportExecDAO.find(executionId);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionId);
        }
        if (!ReportExecStatus.SUCCESS.name().equals(reportExec.getStatus()) || reportExec.getExecResult() == null) {
            SyncopeClientCompositeErrorException sccee =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.InvalidReportExec);
            sce.addElement(reportExec.getExecResult() == null
                    ? "No report data produced"
                    : "Report did not run successfully");
            sccee.addException(sce);
            throw sccee;
        }
        return reportExec;
    }

    @PreAuthorize("hasRole('REPORT_EXECUTE')")
    @RequestMapping(method = RequestMethod.POST, value = "/execute/{reportId}")
    public ReportExecTO execute(@PathVariable("reportId") final Long reportId) {
        Report report = reportDAO.find(reportId);
        if (report == null) {
            throw new NotFoundException("Report " + reportId);
        }

        ReportExecTO result;

        LOG.debug("Triggering new execution of report {}", report);

        try {
            jobInstanceLoader.registerJob(report);

            scheduler.getScheduler().triggerJob(
                    new JobKey(JobInstanceLoader.getJobName(report), Scheduler.DEFAULT_GROUP));
        } catch (Exception e) {
            LOG.error("While executing report {}", report, e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        result = new ReportExecTO();
        result.setReport(reportId);
        result.setStartDate(new Date());
        result.setStatus(ReportExecStatus.STARTED.name());
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @PreAuthorize("hasRole('REPORT_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{reportId}")
    public ReportTO delete(@PathVariable("reportId") final Long reportId) {
        Report report = reportDAO.find(reportId);
        if (report == null) {
            throw new NotFoundException("Report " + reportId);
        }

        ReportTO deletedReport = binder.getReportTO(report);
        jobInstanceLoader.unregisterJob(report);
        reportDAO.delete(report);
        return deletedReport;
    }

    @PreAuthorize("hasRole('REPORT_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/delete/{executionId}")
    public ReportExecTO deleteExecution(@PathVariable("executionId") final Long executionId) {
        ReportExec reportExec = reportExecDAO.find(executionId);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionId);
        }

        ReportExecTO reportExecToDelete = binder.getReportExecTO(reportExec);
        reportExecDAO.delete(reportExec);
        return reportExecToDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ReportTO resolveReference(final Method method, final Object... args) throws
            UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args) && ("create".equals(method.getName())
                || "createInternal".equals(method.getName())
                || "update".equals(method.getName())
                || "delete".equals(method.getName()))) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof ReportTO) {
                    id = ((ReportTO) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getReportTO(reportDAO.find(id));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
