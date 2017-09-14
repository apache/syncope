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
package org.apache.syncope.common.lib.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.ParseException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditElements.Result;

public class AuditLoggerName extends AbstractBaseBean {

    private static final long serialVersionUID = -647989486671786839L;

    public static String getAuditLoggerName(final String domain) {
        return LoggerType.AUDIT.getPrefix() + "." + domain;
    }

    public static String getAuditEventLoggerName(final String domain, final String loggerName) {
        return domain + "." + loggerName;
    }

    private final AuditElements.EventCategoryType type;

    private final String category;

    private final String subcategory;

    private final String event;

    private final Result result;

    @JsonCreator
    public AuditLoggerName(
            @JsonProperty("type") final AuditElements.EventCategoryType type,
            @JsonProperty("category") final String category,
            @JsonProperty("subcategory") final String subcategory,
            @JsonProperty("event") final String event,
            @JsonProperty("result") final Result result) {

        super();

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
        return new StringBuilder().append(LoggerType.AUDIT.getPrefix()).append('.').
                append(buildEvent(type, category, subcategory, event, result)).toString();
    }

    public static AuditLoggerName fromLoggerName(final String loggerName)
            throws ParseException {

        if (StringUtils.isBlank(loggerName)) {
            throw new IllegalArgumentException("Null value not permitted");
        }

        if (!loggerName.startsWith(LoggerType.AUDIT.getPrefix())) {
            throw new ParseException("Audit logger name must start with " + LoggerType.AUDIT.getPrefix(), 0);
        }

        final Map.Entry<EventCategoryTO, Result> eventCategory = parseEventCategory(
                loggerName.replaceAll(LoggerType.AUDIT.getPrefix() + ".", ""));

        return new AuditLoggerName(
                eventCategory.getKey().getType(),
                eventCategory.getKey().getCategory(),
                eventCategory.getKey().getSubcategory(),
                eventCategory.getKey().getEvents().isEmpty()
                ? StringUtils.EMPTY : eventCategory.getKey().getEvents().iterator().next(),
                eventCategory.getValue());
    }

    public static Pair<EventCategoryTO, Result> parseEventCategory(final String event) {
        EventCategoryTO eventCategoryTO = new EventCategoryTO();

        Result condition = null;

        if (StringUtils.isNotEmpty(event)) {
            final String[] elements = event.substring(1, event.length() - 1).split("\\]:\\[");

            if (elements.length == 1) {
                eventCategoryTO.setType(EventCategoryType.CUSTOM);
                condition = Result.SUCCESS;
                eventCategoryTO.getEvents().add(event);
            } else {
                EventCategoryType type;

                if (EventCategoryType.PROPAGATION.toString().equals(elements[0])) {
                    type = EventCategoryType.PROPAGATION;
                } else if (EventCategoryType.PULL.toString().equals(elements[0])) {
                    type = EventCategoryType.PULL;
                } else if (EventCategoryType.PUSH.toString().equals(elements[0])) {
                    type = EventCategoryType.PUSH;
                } else {
                    try {
                        type = EventCategoryType.valueOf(elements[0]);
                    } catch (Exception e) {
                        type = EventCategoryType.CUSTOM;
                    }
                }

                eventCategoryTO.setType(type);

                eventCategoryTO.setCategory(StringUtils.isNotEmpty(elements[1]) ? elements[1] : null);

                eventCategoryTO.setSubcategory(StringUtils.isNotEmpty(elements[2]) ? elements[2] : null);

                if (elements.length > 3 && StringUtils.isNotEmpty(elements[3])) {
                    eventCategoryTO.getEvents().add(elements[3]);
                }

                if (elements.length > 4) {
                    condition = Result.valueOf(elements[4].toUpperCase());
                }
            }
        }

        return Pair.of(eventCategoryTO, condition);
    }

    /**
     * Build event string with the following syntax [type]:[category]:[subcategory]:[event]:[maybe result value cond].
     *
     * @param type event type.
     * @param category event category.
     * @param subcategory event subcategory.
     * @param event event.
     * @param condition result value condition.
     * @return event string.
     */
    public static String buildEvent(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final AuditElements.Result condition) {

        final StringBuilder eventBuilder = new StringBuilder();

        eventBuilder.append('[');
        if (type != null) {
            if (StringUtils.isNotBlank(type.toString())) {
                eventBuilder.append(type.toString());
            } else {
                eventBuilder.append(type.name());
            }
        }
        eventBuilder.append("]:[");
        if (StringUtils.isNotBlank(category)) {
            eventBuilder.append(category);
        }
        eventBuilder.append("]:[");
        if (StringUtils.isNotBlank(subcategory)) {
            eventBuilder.append(subcategory);
        }
        eventBuilder.append("]:[");
        if (StringUtils.isNotBlank(event)) {
            eventBuilder.append(event);
        }
        eventBuilder.append(']');

        if (condition != null) {
            eventBuilder.append(":[").
                    append(condition).
                    append(']');
        }

        return eventBuilder.toString();
    }
}
