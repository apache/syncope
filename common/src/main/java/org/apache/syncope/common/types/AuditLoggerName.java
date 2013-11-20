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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.text.ParseException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.util.LoggerEventUtils;

public class AuditLoggerName extends AbstractBaseBean {

    private static final long serialVersionUID = -647989486671786839L;

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

        this.type = type == null ? AuditElements.EventCategoryType.CUSTOM : type;
        this.category = category;
        this.subcategory = subcategory;
        this.event = event;
        this.result = result == null ? Result.SUCCESS : result;
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
                LoggerType.AUDIT.getPrefix()).append('.').append(
                LoggerEventUtils.buildEvent(type, category, subcategory, event, result)).toString();
    }

    @SuppressWarnings("unchecked")
    public static AuditLoggerName fromLoggerName(final String loggerName)
            throws IllegalArgumentException, ParseException {

        if (StringUtils.isBlank(loggerName)) {
            throw new IllegalArgumentException("Null value not permitted");
        }

        if (!loggerName.startsWith(LoggerType.AUDIT.getPrefix())) {
            throw new ParseException("Audit logger name must start with " + LoggerType.AUDIT.getPrefix(), 0);
        }

        final Map.Entry<EventCategoryTO, Result> eventCategory = LoggerEventUtils.parseEventCategory(
                loggerName.replaceAll(LoggerType.AUDIT.getPrefix() + ".", ""));

        return new AuditLoggerName(
                eventCategory.getKey().getType(),
                eventCategory.getKey().getCategory(),
                eventCategory.getKey().getSubcategory(),
                eventCategory.getKey().getEvents().isEmpty()
                ? StringUtils.EMPTY : eventCategory.getKey().getEvents().iterator().next(),
                eventCategory.getValue());
    }
}
