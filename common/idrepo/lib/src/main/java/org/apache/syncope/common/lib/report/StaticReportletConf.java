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
package org.apache.syncope.common.lib.report;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.TraceLevel;

public class StaticReportletConf extends AbstractReportletConf {

    private static final long serialVersionUID = -4814950086361753689L;

    private String stringField;

    private Long longField;

    private Double doubleField;

    private OffsetDateTime dateField;

    private TraceLevel traceLevel;

    private final List<String> listField = new ArrayList<>();

    public StaticReportletConf() {
        super();
    }

    public StaticReportletConf(final String name) {
        super(name);
    }

    public OffsetDateTime getDateField() {
        return dateField;
    }

    public void setDateField(final OffsetDateTime dateField) {
        this.dateField = dateField;
    }

    public Double getDoubleField() {
        return doubleField;
    }

    public void setDoubleField(final Double doubleField) {
        this.doubleField = doubleField;
    }

    @JacksonXmlElementWrapper(localName = "listField")
    @JacksonXmlProperty(localName = "field")
    public List<String> getListField() {
        return listField;
    }

    public Long getLongField() {
        return longField;
    }

    public void setLongField(final Long longField) {
        this.longField = longField;
    }

    public String getStringField() {
        return stringField;
    }

    public void setStringField(final String stringField) {
        this.stringField = stringField;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(final TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }
}
