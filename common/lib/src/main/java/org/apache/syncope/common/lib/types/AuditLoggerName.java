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

import java.text.ParseException;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditElements.Result;

@XmlRootElement(name = "auditLoggerName")
@XmlType
public class AuditLoggerName extends AbstractBaseBean {

    private static final long serialVersionUID = -647989486671786839L;

    public static String getAuditLoggerName(final String domain) {
        return LoggerType.AUDIT.getPrefix() + "." + domain;
    }

    public static String getAuditEventLoggerName(final String domain, final String loggerName) {
        return domain + "." + loggerName;
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

        return new AuditLoggerName.Builder().
                type(eventCategory.getKey().getType()).
                category(eventCategory.getKey().getCategory()).
                subcategory(eventCategory.getKey().getSubcategory()).
                event(eventCategory.getKey().getEvents().isEmpty()
                        ? StringUtils.EMPTY : eventCategory.getKey().getEvents().iterator().next()).
                result(eventCategory.getValue()).
                build();
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
            eventBuilder.append(type.toString());
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

    public static class Builder {

        private final AuditLoggerName instance = new AuditLoggerName();

        public Builder type(final AuditElements.EventCategoryType type) {
            instance.type = type;
            return this;
        }

        public Builder category(final String category) {
            instance.category = category;
            return this;
        }

        public Builder subcategory(final String subcategory) {
            instance.subcategory = subcategory;
            return this;
        }

        public Builder event(final String event) {
            instance.event = event;
            return this;
        }

        public Builder result(final Result result) {
            instance.result = result;
            return this;
        }

        public AuditLoggerName build() {
            return instance;
        }
    }

    private EventCategoryType type = AuditElements.EventCategoryType.CUSTOM;

    private String category;

    private String subcategory;

    private String event;

    private Result result = Result.SUCCESS;

    public EventCategoryType getType() {
        return type;
    }

    public void setType(final EventCategoryType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(final String subcategory) {
        this.subcategory = subcategory;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(final String event) {
        this.event = event;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(final Result result) {
        this.result = result;
    }

    public String toLoggerName() {
        return new StringBuilder().append(LoggerType.AUDIT.getPrefix()).append('.').
                append(buildEvent(type, category, subcategory, event, result)).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(category).
                append(subcategory).
                append(event).
                append(result).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AuditLoggerName other = (AuditLoggerName) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(category, other.category).
                append(subcategory, other.subcategory).
                append(event, other.event).
                append(result, other.result).
                build();
    }
}
