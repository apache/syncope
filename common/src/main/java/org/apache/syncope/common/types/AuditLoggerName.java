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
package org.apache.syncope.common.types;

import java.text.ParseException;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.util.LoggerEventUtils;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType
@XmlRootElement
public class AuditLoggerName extends AbstractBaseBean {

    private static final long serialVersionUID = -647989486671786839L;

    /**
     * Logger.
     */
    private static Logger LOG = LoggerFactory.getLogger(AuditLoggerName.class);

    private final AuditElements.EventCategoryType type;

    private final String category;

    private final String subcategory;

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    private final String event;

    private final Result result;

    @JsonCreator
    public AuditLoggerName(
            @JsonProperty("type") final AuditElements.EventCategoryType type,
            @JsonProperty("category") final String category,
            @JsonProperty("subcategory") final String subcategory,
            @JsonProperty("event") final String event,
            @JsonProperty("result") final Result result)
            throws IllegalArgumentException {

        if (type == null || result == null) {
            throw new IllegalArgumentException("Null values not permitted");
        }

        this.type = type;
        this.category = category;
        this.subcategory = subcategory;
        this.event = event;
        this.result = result;
    }

    public AuditElements.EventCategoryType getType() {
        return type;
    }

    public String getEvent() {
        return event;
    }

    public String getCategory() {
        return category;
    }

    public Result getResult() {
        return result;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String toLoggerName() {
        return new StringBuilder().append(
                SyncopeLoggerType.AUDIT.getPrefix()).append('.').append(
                LoggerEventUtils.buildEvent(type, category, subcategory, event, result)).toString();
    }

    @SuppressWarnings("unchecked")
    public static AuditLoggerName fromLoggerName(final String loggerName)
            throws IllegalArgumentException, ParseException {

        if (StringUtils.isBlank(loggerName)) {
            throw new IllegalArgumentException("Null value not permitted");
        }

        if (!loggerName.startsWith(SyncopeLoggerType.AUDIT.getPrefix())) {
            throw new ParseException("Audit logger name must start with " + SyncopeLoggerType.AUDIT.getPrefix(), 0);
        }

        final Map.Entry<EventCategoryTO, Result> eventCategory = LoggerEventUtils.parseEventCategory(
                loggerName.replaceAll(SyncopeLoggerType.AUDIT.getPrefix() + ".", ""));

        LOG.debug("From logger name {} to event category {}", loggerName, eventCategory);

        return new AuditLoggerName(
                eventCategory.getKey().getType(),
                eventCategory.getKey().getCategory(),
                eventCategory.getKey().getSubcategory(),
                eventCategory.getKey().getEvents().iterator().next(),
                eventCategory.getValue());
    }
}
