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

import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.report.StaticReportletConf;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.springframework.util.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@ReportletConfClass(StaticReportletConf.class)
public class StaticReportlet extends AbstractReportlet {

    private StaticReportletConf conf;

    private static void doExtractConf(final ContentHandler handler) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        handler.startElement("", "", "configurations", null);
        handler.startElement("", "", "staticAttributes", atts);

        handler.startElement("", "", "string", atts);
        handler.characters("string".toCharArray(), 0, "string".length());
        handler.endElement("", "", "string");

        handler.startElement("", "", "long", atts);
        handler.characters("long".toCharArray(), 0, "long".length());
        handler.endElement("", "", "long");

        handler.startElement("", "", "double", atts);
        handler.characters("double".toCharArray(), 0, "double".length());
        handler.endElement("", "", "double");

        handler.startElement("", "", "date", atts);
        handler.characters("date".toCharArray(), 0, "date".length());
        handler.endElement("", "", "date");

        handler.startElement("", "", "double", atts);
        handler.characters("double".toCharArray(), 0, "double".length());
        handler.endElement("", "", "double");

        handler.startElement("", "", "enum", atts);
        handler.characters("enum".toCharArray(), 0, "enum".length());
        handler.endElement("", "", "enum");

        handler.startElement("", "", "list", atts);
        handler.characters("list".toCharArray(), 0, "list".length());
        handler.endElement("", "", "list");

        handler.endElement("", "", "staticAttributes");
        handler.endElement("", "", "configurations");
    }

    @Override
    protected void doExtract(
            final ReportletConf conf,
            final ContentHandler handler,
            final AtomicReference<String> status)
            throws SAXException {

        if (conf instanceof StaticReportletConf) {
            this.conf = StaticReportletConf.class.cast(conf);
        } else {
            throw new ReportException(new IllegalArgumentException("Invalid configuration provided"));
        }

        doExtractConf(handler);

        if (StringUtils.hasText(this.conf.getStringField())) {
            handler.startElement("", "", "string", null);
            handler.characters(this.conf.getStringField().toCharArray(), 0, this.conf.getStringField().length());
            handler.endElement("", "", "string");
        }

        if (this.conf.getLongField() != null) {
            handler.startElement("", "", "long", null);
            String printed = String.valueOf(this.conf.getLongField());
            handler.characters(printed.toCharArray(), 0, printed.length());
            handler.endElement("", "", "long");
        }

        if (this.conf.getDoubleField() != null) {
            handler.startElement("", "", "double", null);
            String printed = String.valueOf(this.conf.getDoubleField());
            handler.characters(printed.toCharArray(), 0, printed.length());
            handler.endElement("", "", "double");
        }

        if (this.conf.getDateField() != null) {
            handler.startElement("", "", "date", null);
            String printed = FormatUtils.format(this.conf.getDateField());
            handler.characters(printed.toCharArray(), 0, printed.length());
            handler.endElement("", "", "date");
        }

        if (this.conf.getTraceLevel() != null) {
            handler.startElement("", "", "enum", null);
            String printed = this.conf.getTraceLevel().name();
            handler.characters(printed.toCharArray(), 0, printed.length());
            handler.endElement("", "", "enum");
        }

        if (this.conf.getListField() != null && !this.conf.getListField().isEmpty()) {
            handler.startElement("", "", "list", null);
            for (String item : this.conf.getListField()) {
                if (StringUtils.hasText(item)) {
                    handler.startElement("", "", "string", null);
                    handler.characters(item.toCharArray(), 0, item.length());
                    handler.endElement("", "", "string");
                }
            }
            handler.endElement("", "", "list");
        }
    }
}
