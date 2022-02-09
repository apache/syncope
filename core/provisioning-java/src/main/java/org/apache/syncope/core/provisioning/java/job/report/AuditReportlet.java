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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.report.AuditReportletConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;

@ReportletConfClass(AuditReportletConf.class)
public class AuditReportlet extends AbstractReportlet {

    @Autowired
    private DomainHolder domainHolder;

    private AuditReportletConf conf;

    private DataSource datasource;

    private void doExtractConf(final ContentHandler handler, final AtomicReference<String> status) throws SAXException {
        status.set("Fetching " + conf.getSize() + " rows from the " + AuditConfDAO.AUDIT_ENTRY_TABLE + " table");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
        jdbcTemplate.setMaxRows(conf.getSize());
        List<Map<String, Object>> rows = jdbcTemplate.
                queryForList("SELECT * FROM " + AuditConfDAO.AUDIT_ENTRY_TABLE + " ORDER BY EVENT_DATE DESC");

        handler.startElement("", "", "events", null);
        AttributesImpl atts = new AttributesImpl();
        for (Map<String, Object> row : rows) {
            AuditEntry auditEntry = POJOHelper.deserialize(row.get("MESSAGE").toString(), AuditEntry.class);

            atts.clear();
            if (StringUtils.isNotBlank(auditEntry.getWho())) {
                atts.addAttribute("", "", "who", ReportXMLConst.XSD_STRING, auditEntry.getWho());
            }
            handler.startElement("", "", "event", atts);

            atts.clear();
            if (StringUtils.isNotBlank(auditEntry.getLogger().getCategory())) {
                atts.addAttribute("", "", "category",
                        ReportXMLConst.XSD_STRING, auditEntry.getLogger().getCategory());
            }
            if (StringUtils.isNotBlank(auditEntry.getLogger().getSubcategory())) {
                atts.addAttribute("", "", "subcategory",
                        ReportXMLConst.XSD_STRING, auditEntry.getLogger().getSubcategory());
            }
            if (StringUtils.isNotBlank(auditEntry.getLogger().getEvent())) {
                atts.addAttribute("", "", "event",
                        ReportXMLConst.XSD_STRING, auditEntry.getLogger().getEvent());
            }
            if (auditEntry.getLogger().getResult() != null) {
                atts.addAttribute("", "", "result",
                        ReportXMLConst.XSD_STRING, auditEntry.getLogger().getResult().name());
            }
            handler.startElement("", "", "logger", atts);
            handler.endElement("", "", "logger");

            if (auditEntry.getBefore() != null) {
                char[] before = POJOHelper.serialize(auditEntry.getBefore()).toCharArray();
                handler.startElement("", "", "before", null);
                handler.characters(before, 0, before.length);
                handler.endElement("", "", "before");
            }

            if (!auditEntry.getInputs().isEmpty()) {
                handler.startElement("", "", "inputs", null);
                for (String input : auditEntry.getInputs()) {
                    handler.startElement("", "", "input", null);
                    handler.characters(input.toCharArray(), 0, input.length());
                    handler.endElement("", "", "input");
                }
                handler.endElement("", "", "inputs");
            }

            if (auditEntry.getOutput() != null) {
                char[] output = POJOHelper.serialize(auditEntry.getOutput()).toCharArray();
                handler.startElement("", "", "output", null);
                handler.characters(output, 0, output.length);
                handler.endElement("", "", "output");
            }

            handler.startElement("", "", "throwable", null);
            Object throwableValue = row.get("THROWABLE");
            if (throwableValue != null) {
                char[] throwable = throwableValue.toString().toCharArray();
                handler.characters(throwable, 0, throwable.length);
            }
            handler.endElement("", "", "throwable");

            handler.endElement("", "", "event");
        }
        handler.endElement("", "", "events");

        status.set("Fetched " + conf.getSize() + " rows from the " + AuditConfDAO.AUDIT_ENTRY_TABLE + " table");
    }

    @Override
    protected void doExtract(
            final ReportletConf conf,
            final ContentHandler handler,
            final AtomicReference<String> status)
            throws SAXException {

        if (conf instanceof AuditReportletConf) {
            this.conf = AuditReportletConf.class.cast(conf);
        } else {
            throw new ReportException(new IllegalArgumentException("Invalid configuration provided"));
        }

        datasource = domainHolder.getDomains().get(AuthContextUtils.getDomain());
        if (datasource == null) {
            throw new ReportException(new IllegalArgumentException("Could not get to DataSource"));
        }

        doExtractConf(handler, status);
    }
}
