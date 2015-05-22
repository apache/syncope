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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;
import org.apache.cocoon.optional.pipeline.components.sax.fop.FopSerializer;
import org.apache.cocoon.pipeline.NonCachingPipeline;
import org.apache.cocoon.pipeline.Pipeline;
import org.apache.cocoon.sax.SAXPipelineComponent;
import org.apache.cocoon.sax.component.XMLGenerator;
import org.apache.cocoon.sax.component.XMLSerializer;
import org.apache.cocoon.sax.component.XSLTTransformer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.logic.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.provisioning.api.job.JobInstanceLoader;
import org.apache.syncope.core.logic.report.Reportlet;
import org.apache.syncope.core.logic.report.ReportletConfClass;
import org.apache.syncope.core.logic.report.TextSerializer;
import org.apache.syncope.common.lib.CollectionUtils2;
import org.apache.syncope.common.lib.to.AbstractExecTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.xmlgraphics.util.MimeConstants;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

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

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @PreAuthorize("hasRole('" + Entitlement.REPORT_CREATE + "')")
    public ReportTO create(final ReportTO reportTO) {
        Report report = entityFactory.newEntity(Report.class);
        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_UPDATE + "')")
    public ReportTO update(final ReportTO reportTO) {
        Report report = reportDAO.find(reportTO.getKey());
        if (report == null) {
            throw new NotFoundException("Report " + reportTO.getKey());
        }

        binder.getReport(report, reportTO);
        report = reportDAO.save(report);

        try {
            jobInstanceLoader.registerJob(report);
        } catch (Exception e) {
            LOG.error("While registering quartz job for report " + report.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_LIST + "')")
    public int count() {
        return reportDAO.count();
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_LIST + "')")
    public List<ReportTO> list(final int page, final int size, final List<OrderByClause> orderByClauses) {
        return CollectionUtils.collect(reportDAO.findAll(page, size, orderByClauses),
                new Transformer<Report, ReportTO>() {

                    @Override
                    public ReportTO transform(final Report input) {
                        return binder.getReportTO(input);
                    }
                }, new ArrayList<ReportTO>());
    }

    private Class<? extends ReportletConf> getReportletConfClass(final Class<Reportlet> reportletClass) {
        Class<? extends ReportletConf> result = null;

        ReportletConfClass annotation = reportletClass.getAnnotation(ReportletConfClass.class);
        if (annotation != null) {
            result = annotation.value();
        }

        return result;
    }

    @SuppressWarnings({ "rawtypes" })
    private Set<Class<Reportlet>> getAllReportletClasses() {
        return CollectionUtils2.collect(classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.REPORTLET),
                new Transformer<String, Class<Reportlet>>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public Class<Reportlet> transform(final String className) {
                        Class<Reportlet> result = null;
                        try {
                            Class reportletClass = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
                            result = reportletClass;
                        } catch (ClassNotFoundException e) {
                            LOG.warn("Could not load class {}", className);
                        } catch (LinkageError e) {
                            LOG.warn("Could not link class {}", className);
                        }

                        return result;
                    }
                },
                PredicateUtils.notNullPredicate(), new HashSet<Class<Reportlet>>());
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_LIST + "')")
    public Set<String> getReportletConfClasses() {
        return CollectionUtils2.collect(getAllReportletClasses(),
                new Transformer<Class<Reportlet>, String>() {

                    @Override
                    public String transform(final Class<Reportlet> reportletClass) {
                        Class<? extends ReportletConf> reportletConfClass = getReportletConfClass(reportletClass);
                        return reportletConfClass == null ? null : reportletConfClass.getName();
                    }
                }, PredicateUtils.notNullPredicate(), new HashSet<String>());
    }

    public Class<Reportlet> findReportletClassHavingConfClass(final Class<? extends ReportletConf> reportletConfClass) {
        Class<Reportlet> result = null;
        for (Class<Reportlet> reportletClass : getAllReportletClasses()) {
            Class<? extends ReportletConf> found = getReportletConfClass(reportletClass);
            if (found != null && found.equals(reportletConfClass)) {
                result = reportletClass;
            }
        }

        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_READ + "')")
    public ReportTO read(final Long reportKey) {
        Report report = reportDAO.find(reportKey);
        if (report == null) {
            throw new NotFoundException("Report " + reportKey);
        }
        return binder.getReportTO(report);
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_READ + "')")
    @Transactional(readOnly = true)
    public ReportExecTO readExecution(final Long executionKey) {
        ReportExec reportExec = reportExecDAO.find(executionKey);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionKey);
        }
        return binder.getReportExecTO(reportExec);
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_READ + "')")
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

    @PreAuthorize("hasRole('" + Entitlement.REPORT_READ + "')")
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

    @PreAuthorize("hasRole('" + Entitlement.REPORT_EXECUTE + "')")
    public ReportExecTO execute(final Long reportKey) {
        Report report = reportDAO.find(reportKey);
        if (report == null) {
            throw new NotFoundException("Report " + reportKey);
        }

        try {
            jobInstanceLoader.registerJob(report);

            scheduler.getScheduler().triggerJob(
                    new JobKey(JobNamer.getJobName(report), Scheduler.DEFAULT_GROUP));
        } catch (Exception e) {
            LOG.error("While executing report {}", report, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        ReportExecTO result = new ReportExecTO();
        result.setReport(reportKey);
        result.setStartDate(new Date());
        result.setStatus(ReportExecStatus.STARTED.name());
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_DELETE + "')")
    public ReportTO delete(final Long reportKey) {
        Report report = reportDAO.find(reportKey);
        if (report == null) {
            throw new NotFoundException("Report " + reportKey);
        }

        ReportTO deletedReport = binder.getReportTO(report);
        jobInstanceLoader.unregisterJob(report);
        reportDAO.delete(report);
        return deletedReport;
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_DELETE + "')")
    public ReportExecTO deleteExecution(final Long executionKey) {
        ReportExec reportExec = reportExecDAO.find(executionKey);
        if (reportExec == null) {
            throw new NotFoundException("Report execution " + executionKey);
        }

        ReportExecTO reportExecToDelete = binder.getReportExecTO(reportExec);
        reportExecDAO.delete(reportExec);
        return reportExecToDelete;
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

    @Override
    @PreAuthorize("hasRole('" + Entitlement.REPORT_LIST + "')")
    public <E extends AbstractExecTO> List<E> list(final JobStatusType type, final Class<E> reference) {
        return super.list(type, reference);
    }

    @PreAuthorize("hasRole('" + Entitlement.REPORT_EXECUTE + "')")
    public void process(final JobAction action, final Long reportKey) {
        Report report = reportDAO.find(reportKey);
        if (report == null) {
            throw new NotFoundException("Report " + reportKey);
        }
        String jobName = JobNamer.getJobName(report);
        process(action, jobName);
    }

    @Override
    protected Long getKeyFromJobName(final JobKey jobKey) {
        return JobNamer.getReportKeyFromJobName(jobKey.getName());
    }
}
