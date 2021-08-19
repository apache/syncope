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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.spring.ImplementationManager;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.helpers.AttributesImpl;

public class DefaultReportJobDelegate implements ReportJobDelegate {

    protected static final Logger LOG = LoggerFactory.getLogger(ReportJobDelegate.class);

    protected static final SAXTransformerFactory TRANSFORMER_FACTORY;

    static {
        TRANSFORMER_FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance();
        TRANSFORMER_FACTORY.setURIResolver((href, base) -> null);
        try {
            TRANSFORMER_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            LOG.error("Could not enable secure XML processing", e);
        }
    }

    protected final ReportDAO reportDAO;

    protected final ReportExecDAO reportExecDAO;

    protected final EntityFactory entityFactory;

    protected final AtomicReference<String> status = new AtomicReference<>();

    protected boolean interrupt;

    protected boolean interrupted;

    public DefaultReportJobDelegate(
            final ReportDAO reportDAO,
            final ReportExecDAO reportExecDAO,
            final EntityFactory entityFactory) {

        this.reportDAO = reportDAO;
        this.reportExecDAO = reportExecDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public String currentStatus() {
        return status.get();
    }

    @Override
    public void interrupt() {
        interrupt = true;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    @Transactional
    @Override
    public void execute(final String reportKey, final String executor) throws JobExecutionException {
        Report report = reportDAO.find(reportKey);
        if (report == null) {
            throw new JobExecutionException("Report " + reportKey + " not found");
        }

        if (!report.isActive()) {
            LOG.info("Report {} not active, aborting...", reportKey);
            return;
        }

        // 1. create execution
        ReportExec execution = entityFactory.newEntity(ReportExec.class);
        execution.setStatus(ReportExecStatus.STARTED);
        execution.setStart(new Date());
        execution.setReport(report);

        execution.setExecutor(executor);
        execution = reportExecDAO.save(execution);

        report.add(execution);
        report = reportDAO.save(report);

        // 2. define a SAX handler for generating result as XML
        TransformerHandler handler;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        zos.setLevel(Deflater.BEST_COMPRESSION);
        try {
            handler = TRANSFORMER_FACTORY.newTransformerHandler();
            Transformer serializer = handler.getTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");

            // a single ZipEntry in the ZipOutputStream
            zos.putNextEntry(new ZipEntry(report.getName()));

            // streaming SAX handler in a compressed byte array stream
            handler.setResult(new StreamResult(zos));
        } catch (Exception e) {
            throw new JobExecutionException("While configuring for SAX generation", e, true);
        }

        execution.setStatus(ReportExecStatus.RUNNING);
        execution = reportExecDAO.save(execution);

        status.set("Starting");

        // 3. actual report execution
        StringBuilder reportExecutionMessage = new StringBuilder();
        try {
            // report header
            handler.startDocument();
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, report.getName());
            handler.startElement("", "", ReportXMLConst.ELEMENT_REPORT, atts);

            status.set("Generating report header");

            // iterate over reportlet instances defined for this report
            for (int i = 0; i < report.getReportlets().size() && !interrupt; i++) {
                Optional<Reportlet> reportlet = ImplementationManager.buildReportlet(report.getReportlets().get(i));
                if (reportlet.isPresent()) {
                    try {
                        status.set("Invoking reportlet " + report.getReportlets().get(i).getKey());
                        reportlet.get().extract(handler, status);
                    } catch (Throwable t) {
                        LOG.error("While executing reportlet {} for report {}", reportlet, reportKey, t);

                        execution.setStatus(ReportExecStatus.FAILURE);

                        Throwable effective = t instanceof ReportException
                                ? t.getCause()
                                : t;
                        reportExecutionMessage.
                                append(ExceptionUtils2.getFullStackTrace(effective)).
                                append("\n==================\n");
                    }
                }
            }
            if (interrupt) {
                LOG.debug("Report job {} interrupted", reportKey);
                interrupted = true;
            }

            // report footer
            status.set("Generating report footer");

            handler.endElement("", "", ReportXMLConst.ELEMENT_REPORT);
            handler.endDocument();

            if (!ReportExecStatus.FAILURE.name().equals(execution.getStatus())) {
                execution.setStatus(ReportExecStatus.SUCCESS);
            }
        } catch (Exception e) {
            execution.setStatus(ReportExecStatus.FAILURE);
            reportExecutionMessage.append(ExceptionUtils2.getFullStackTrace(e));

            throw new JobExecutionException(e, true);
        } finally {
            status.set("Completed");

            try {
                zos.closeEntry();
                zos.close();
                baos.close();
            } catch (IOException e) {
                LOG.error("While closing StreamResult's backend", e);
            }

            execution.setExecResult(baos.toByteArray());
            execution.setMessage(reportExecutionMessage.toString());
            execution.setEnd(new Date());
            reportExecDAO.save(execution);
        }
    }
}
