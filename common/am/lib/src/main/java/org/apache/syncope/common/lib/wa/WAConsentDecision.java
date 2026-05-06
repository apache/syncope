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
package org.apache.syncope.common.lib.wa;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class WAConsentDecision implements BaseBean {

    private static final long serialVersionUID = -4763224069061622840L;

    public static class Builder {

        private final WAConsentDecision instance;

        public Builder(final long id, final String principal, final String service, final LocalDateTime createDate) {
            instance = new WAConsentDecision();
            instance.setId(id);
            instance.setPrincipal(principal);
            instance.setService(service);
            instance.setCreatedDate(createDate);
        }

        public Builder options(final ReminderOptions options) {
            instance.setOptions(options);
            return this;
        }

        public Builder reminder(final long reminder) {
            instance.setReminder(reminder);
            return this;
        }

        public Builder reminderTimeUnit(final ChronoUnit reminderTimeUnit) {
            instance.setReminderTimeUnit(reminderTimeUnit);
            return this;
        }

        public Builder attributes(final String attributes) {
            instance.setAttributes(attributes);
            return this;
        }

        public WAConsentDecision build() {
            return instance;
        }
    }

    public enum ReminderOptions {
        /**
         * Always ask for consent.
         */
        ALWAYS(0),
        /**
         * Ask for consent when there is modification in one of the attribute names or if consent is expired.
         */
        ATTRIBUTE_NAME(1),
        /**
         * Ask for consent when there is modification in one of the attribute names, the values contain inside the
         * attributes or if consent is expired.
         */
        ATTRIBUTE_VALUE(2);

        private final int value;

        ReminderOptions(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private long id;

    private String principal;

    private String service;

    private LocalDateTime createdDate;

    private ReminderOptions options = ReminderOptions.ATTRIBUTE_NAME;

    private long reminder = 14L;

    private ChronoUnit reminderTimeUnit = ChronoUnit.DAYS;

    private String attributes;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(final String principal) {
        this.principal = principal;
    }

    public String getService() {
        return service;
    }

    public void setService(final String service) {
        this.service = service;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ReminderOptions getOptions() {
        return options;
    }

    public void setOptions(final ReminderOptions options) {
        this.options = options;
    }

    public long getReminder() {
        return reminder;
    }

    public void setReminder(final long reminder) {
        this.reminder = reminder;
    }

    public ChronoUnit getReminderTimeUnit() {
        return reminderTimeUnit;
    }

    public void setReminderTimeUnit(final ChronoUnit reminderTimeUnit) {
        this.reminderTimeUnit = reminderTimeUnit;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(final String attributes) {
        this.attributes = attributes;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(id).
                append(principal).
                append(service).
                append(createdDate).
                append(options).
                append(reminder).
                append(reminderTimeUnit).
                append(attributes).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        WAConsentDecision other = (WAConsentDecision) obj;
        return new EqualsBuilder().
                append(id, other.id).
                append(principal, other.principal).
                append(service, other.service).
                append(createdDate, other.createdDate).
                append(options, other.options).
                append(reminder, other.reminder).
                append(reminderTimeUnit, other.reminderTimeUnit).
                append(attributes, other.attributes).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("id", id).
                append("principal", principal).
                append("service", service).
                append("createdDate", createdDate).
                append("options", options).
                append("reminder", reminder).
                append("reminderTimeUnit", reminderTimeUnit).
                append("attributes", attributes).
                build();
    }
}
