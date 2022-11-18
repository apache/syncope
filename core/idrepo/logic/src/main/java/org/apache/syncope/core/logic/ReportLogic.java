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
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import javax.ws.rs.core.Response;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
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

    protected static final Pattern XSLT_PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][\\w\\-\\.]*");

    protected static final SAXTransformerFactory TRAX_FACTORY;

    protected static final FopFactory FOP_FACTORY = new FopFactoryBuilder(new File(".").toURI()).build();

    static {
        TRAX_FACTORY = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TRAX_FACTORY.setURIResolver((href, base) -> null);
        try {
            TRAX_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            LOG.error("Could not enable secure XML processing", e);
        }
    }

    protected final ReportDAO reportDAO;

    protected final ReportExecDAO reportExecDAO;

    protected final ConfParamOps confParamOps;

    protected final ReportDataBinder binder;

    protected final EntityFactory entityFactory;

    public ReportLogic(
            final JobManager jobManager,
            final SchedulerFactoryBean scheduler,
            final JobStatusDAO jobStatusDAO,
            final ReportDAO reportDAO,
            final ReportExecDAO reportExecDAO,
            final ConfParamOps confParamOps,
            final ReportDataBinder binder,
            final EntityFactory entityFactory) {

        super(jobManager, scheduler, jobStatusDAO);

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

    protected Transformer buildXSLTTransformer(final String template, final Map<String, Object> parameters)
            throws TransformerConfigurationException {

        Templates templates = TRAX_FACTORY.newTemplates(
                new StreamSource(IOUtils.toInputStream(template, StandardCharsets.UTF_8)));
        TransformerHandler transformerHandler = TRAX_FACTORY.newTransformerHandler(templates);

        Transformer transformer = transformerHandler.getTransformer();
        parameters.forEach((name, values) -> {
            if (XSLT_PARAMETER_NAME_PATTERN.matcher(name).matches()) {
                transformer.setParameter(name, values);
            }
        });

        return transformer;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REPORT_READ + "')")
    public void exportExecutionResult(
            final OutputStream os,
            final ReportExec reportExec,
            final ReportExecExportFormat format) {

        // streaming SAX handler from a compressed byte array stream
        try (ByteArrayInputStream bais = new ByteArrayInputStream(reportExec.getExecResult());
             ZipInputStream zis = new ZipInputStream(bais)) {

            // a single ZipEntry in the ZipInputStream (see ReportJob)
            zis.getNextEntry();

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("status", reportExec.getStatus());
            parameters.put("message", reportExec.getMessage());
            parameters.put("start", reportExec.getStart());
            parameters.put("end", reportExec.getEnd());

            switch (format) {
                case HTML:
                    Transformer html = buildXSLTTransformer(
                            reportExec.getReport().getTemplate().getHTMLTemplate(), parameters);
                    html.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    html.transform(
                            new StreamSource(zis),
                            new StreamResult(os));
                    break;

                case PDF:
                    Transformer pdf = buildXSLTTransformer(
                            reportExec.getReport().getTemplate().getFOTemplate(), parameters);
                    pdf.transform(
                            new StreamSource(zis),
                            new SAXResult(FOP_FACTORY.newFop(MimeConstants.MIME_PDF, os).getDefaultHandler()));
                    break;

                case RTF:
                    Transformer rtf = buildXSLTTransformer(
                            reportExec.getReport().getTemplate().getFOTemplate(), parameters);
                    rtf.transform(
                            new StreamSource(zis),
                            new SAXResult(FOP_FACTORY.newFop(MimeConstants.MIME_RTF, os).getDefaultHandler()));
                    break;

                case CSV:
                    Transformer csv = buildXSLTTransformer(
                            reportExec.getReport().getTemplate().getCSVTemplate(), parameters);
                    csv.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    csv.transform(
                            new StreamSource(zis),
                            new StreamResult(os));
                    break;

                case XML:
                default:
                    zis.transferTo(os);
            }

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
            final String key,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        Report report = Optional.ofNullable(reportDAO.find(key)).
                orElseThrow(() -> new NotFoundException("Report " + key));

        Integer count = reportExecDAO.count(report, before, after);

        List<ExecTO> result = reportExecDAO.findAll(report, before, after, page, size, orderByClauses).stream().
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
            final OffsetDateTime before,
            final OffsetDateTime after) {

        Report report = Optional.ofNullable(reportDAO.find(key)).
                orElseThrow(() -> new NotFoundException("Report " + key));

        List<BatchResponseItem> batchResponseItems = new ArrayList<>();

        reportExecDAO.findAll(report, before, after, -1, -1, List.of()).forEach(exec -> {
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

        return Optional.ofNullable(reportDAO.find(key)).
                map(f -> Triple.of(JobType.REPORT, key, binder.buildRefDesc(f))).orElse(null);
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
