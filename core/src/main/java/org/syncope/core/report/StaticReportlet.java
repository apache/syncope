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
package org.syncope.core.report;

import org.springframework.util.StringUtils;
import org.syncope.client.report.StaticReportletConf;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

@ReportletConfClass(StaticReportletConf.class)
public class StaticReportlet extends AbstractReportlet<StaticReportletConf> {

    @Override
    public void doExtract(final ContentHandler handler) throws SAXException, ReportException {

        if (StringUtils.hasText(conf.getStringField())) {
            handler.startElement("", "", "string", null);
            handler.characters(conf.getStringField().toCharArray(), 0, conf.getStringField().length() - 1);
            handler.endElement("", "", "string");
        }

        if (conf.getLongField() != null) {
            handler.startElement("", "", "long", null);
            String printed = String.valueOf(conf.getLongField());
            handler.characters(printed.toCharArray(), 0, printed.length() - 1);
            handler.endElement("", "", "long");
        }

        if (conf.getDoubleField() != null) {
            handler.startElement("", "", "double", null);
            String printed = String.valueOf(conf.getDoubleField());
            handler.characters(printed.toCharArray(), 0, printed.length() - 1);
            handler.endElement("", "", "double");
        }

        if (conf.getDateField() != null) {
            handler.startElement("", "", "date", null);
            String printed = DATE_FORMAT.get().format(conf.getDateField());
            handler.characters(printed.toCharArray(), 0, printed.length() - 1);
            handler.endElement("", "", "date");
        }

        if (conf.getTraceLevel() != null) {
            handler.startElement("", "", "enum", null);
            String printed = conf.getTraceLevel().name();
            handler.characters(printed.toCharArray(), 0, printed.length() - 1);
            handler.endElement("", "", "enum");
        }

        if (conf.getListField() != null && !conf.getListField().isEmpty()) {
            handler.startElement("", "", "list", null);
            for (String item : conf.getListField()) {
                if (StringUtils.hasText(item)) {
                    handler.startElement("", "", "string", null);
                    handler.characters(item.toCharArray(), 0, item.length() - 1);
                    handler.endElement("", "", "string");
                }
            }
            handler.endElement("", "", "list");
        }
    }
}
