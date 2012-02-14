/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.apache.cocoon.sax.builder.SAXPipelineBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.syncope.client.to.ReportExecTO;
import org.syncope.client.to.ReportTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.init.JobInstanceLoader;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.beans.ReportExec;
import org.syncope.core.persistence.dao.ReportDAO;
import org.syncope.core.persistence.dao.ReportExecDAO;
import org.syncope.core.report.AbstractReportlet;
import org.syncope.core.report.Reportlet;
import org.syncope.core.rest.data.ReportDataBinder;
import org.syncope.types.ReportExecExportFormat;
import org.syncope.types.ReportExecStatus;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/report")
public class ReportController extends AbstractController {

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

    @Autowired
    private ResourcePatternResolver resResolver;

    @PreAuthorize("hasRole('REPORT_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ReportTO create(final HttpServletResponse response,
            @RequestBody final ReportTO reportTO) {

        LOG.debug("Creating report " + reportTO);

        Report report = new Report();
        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report "
                    + report.getId(), e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('REPORT_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ReportTO update(@RequestBody final ReportTO reportTO)
            throws NotFoundException {

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
            LOG.error("While registering quartz job for report "
                    + report.getId(), e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/count")
    public ModelAndView count() {
        return new ModelAndView().addObject(reportDAO.count());
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<ReportTO> list() {
        List<Report> reports = reportDAO.findAll();
        List<ReportTO> result = new ArrayList<ReportTO>(reports.size());
        for (Report report : reports) {
            result.add(binder.getReportTO(report));
        }

        return result;
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list/{page}/{size}")
    public List<ReportTO> list(@PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        List<Report> reports = reportDAO.findAll(page, size);
        List<ReportTO> result = new ArrayList<ReportTO>(reports.size());
        for (Report report : reports) {
            result.add(binder.getReportTO(report));
        }

        return result;
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/list")
    public List<ReportExecTO> listExecutions() {
        List<ReportExec> executions = reportExecDAO.findAll();
        List<ReportExecTO> executionTOs =
                new ArrayList<ReportExecTO>(executions.size());
        for (ReportExec execution : executions) {
            executionTOs.add(binder.getReportExecTO(execution));
        }

        return executionTOs;
    }

    @PreAuthorize("hasRole('REPORT_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/reportletClasses")
    public ModelAndView getReportletClasses() {
        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        Set<String> reportletClasses = new HashSet<String>();
        try {
            for (Resource resource : resResolver.getResources(
                    "classpath*:**/*.class")) {

                ClassMetadata metadata =
                        cachingMetadataReaderFactory.getMetadataReader(
                        resource).getClassMetadata();
                if (ArrayUtils.contains(metadata.getInterfaceNames(),
                        Reportlet.class.getName())
                        || AbstractReportlet.class.getName().equals(
                        metadata.getSuperClassName())) {

                    try {
                        Class jobClass = Class.forName(metadata.getClassName());
                        if (!Modifier.isAbstract(jobClass.getModifiers())) {

                            reportletClasses.add(jobClass.getName());
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error("Could not load class {}",
                                metadata.getClassName(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}",
                    Reportlet.class.getName(), e);
        }

        ModelAndView result = new ModelAndView();
        result.addObject(reportletClasses);
        return result;
    }

    @PreAuthorize("hasRole('REPORT_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{reportId}")
    public ReportTO read(@PathVariable("reportId") final Long reportId)
            throws NotFoundException {

        Report report = reportDAO.find(reportId);
        if (report == null) {
            throw new NotFoundException("Report " + reportId);
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('REPORT_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/read/{executionId}")
    @Transactional(readOnly = true)
    public ReportExecTO readExecution(
            @PathVariable("executionId") final Long executionId)
            throws NotFoundException {

        ReportExec execution = reportExecDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Report execution " + executionId);
        }

        return binder.getReportExecTO(execution);
    }

    @PreAuthorize("hasRole('REPORT_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/export/{executionId}")
    @Transactional(readOnly = true)
    public void exportExecutionResult(
            final HttpServletResponse response,
            @PathVariable("executionId") final Long executionId,
            @RequestParam(value = "fmt",
            required = false) final ReportExecExportFormat fmt)
            throws NotFoundException {

        ReportExec reportExec = reportExecDAO.find(executionId);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionId);
        }
        if (!ReportExecStatus.SUCCESS.name().equals(reportExec.getStatus())
                || reportExec.getExecResult() == null) {

            SyncopeClientCompositeErrorException sccee =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidReportExec);
            sce.addElement(reportExec.getExecResult() == null
                    ? "No report data produced"
                    : "Report did not run successfully");
            sccee.addException(sce);
            throw sccee;
        }

        ReportExecExportFormat format =
                fmt == null ? ReportExecExportFormat.XML : fmt;

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.addHeader("Content-Disposition",
                "attachment; filename=" + reportExec.getReport().getName()
                + "." + format.name().toLowerCase());

        // streaming SAX handler from a compressed byte array stream
        ByteArrayInputStream bais =
                new ByteArrayInputStream(reportExec.getExecResult());
        ZipInputStream zis = new ZipInputStream(bais);
        try {
            // a single ZipEntry in the ZipInputStream (see ReportJob)
            zis.getNextEntry();

            SAXPipelineBuilder.newNonCachingPipeline().
                    setInputStreamGenerator(zis).
                    addSerializer().withEmptyConfiguration().
                    setup(response.getOutputStream()).
                    execute();

            LOG.debug("Default content successfully exported");
        } catch (Throwable t) {
            LOG.error("While exporting content", t);
        } finally {
            try {
                zis.close();
                bais.close();
            } catch (IOException e) {
                LOG.error("While closing stream for execution result", e);
            }
        }
    }

    @PreAuthorize("hasRole('REPORT_EXECUTE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/execute/{reportId}")
    public ReportExecTO execute(@PathVariable("reportId") final Long reportId)
            throws NotFoundException {

        Report report = reportDAO.find(reportId);
        if (report == null) {
            throw new NotFoundException("Report " + reportId);
        }

        try {
            jobInstanceLoader.registerJob(report);

            JobDataMap map = new JobDataMap();
            scheduler.getScheduler().triggerJob(
                    JobInstanceLoader.getJobName(report),
                    Scheduler.DEFAULT_GROUP, map);
        } catch (Exception e) {
            LOG.error("While executing report {}", report, e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        ReportExecTO result = new ReportExecTO();
        result.setReport(reportId);
        result.setStartDate(new Date());
        result.setStatus(ReportExecStatus.STARTED);
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @PreAuthorize("hasRole('REPORT_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{reportId}")
    public void delete(@PathVariable("reportId") Long reportId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        Report report = reportDAO.find(reportId);
        if (report == null) {
            throw new NotFoundException("Report " + reportId);
        }

        jobInstanceLoader.unregisterJob(report);

        reportDAO.delete(report);
    }

    @PreAuthorize("hasRole('REPORT_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/execution/delete/{executionId}")
    public void deleteExecution(@PathVariable("executionId") Long executionId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        ReportExec execution = reportExecDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Report execution " + executionId);
        }

        reportExecDAO.delete(execution);
    }
}
