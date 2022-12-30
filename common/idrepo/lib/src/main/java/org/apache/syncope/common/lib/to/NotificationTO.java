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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.TraceLevel;

public class NotificationTO implements EntityTO {

    private static final long serialVersionUID = -6145117115632592612L;

    private String key;

    private final List<String> events = new ArrayList<>();

    private final Map<String, String> abouts = new HashMap<>();

    private String recipientsFIQL;

    private final List<String> staticRecipients = new ArrayList<>();

    private String recipientAttrName;

    private boolean selfAsRecipient;

    private String recipientsProvider;

    private String sender;

    private String subject;

    private String template;

    private TraceLevel traceLevel;

    private boolean active;

    public Map<String, String> getAbouts() {
        return abouts;
    }

    @JacksonXmlElementWrapper(localName = "events")
    @JacksonXmlProperty(localName = "event")
    public List<String> getEvents() {
        return events;
    }

    @JacksonXmlElementWrapper(localName = "staticRecipients")
    @JacksonXmlProperty(localName = "staticRecipient")
    public List<String> getStaticRecipients() {
        return staticRecipients;
    }

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getRecipientsFIQL() {
        return recipientsFIQL;
    }

    public void setRecipientsFIQL(final String recipientsFIQL) {
        this.recipientsFIQL = recipientsFIQL;
    }

    public String getRecipientAttrName() {
        return recipientAttrName;
    }

    public void setRecipientAttrName(final String recipientAttrName) {
        this.recipientAttrName = recipientAttrName;
    }

    public boolean isSelfAsRecipient() {
        return selfAsRecipient;
    }

    public void setSelfAsRecipient(final boolean selfAsRecipient) {
        this.selfAsRecipient = selfAsRecipient;
    }

    public String getRecipientsProvider() {
        return recipientsProvider;
    }

    public void setRecipientsProvider(final String recipientsProvider) {
        this.recipientsProvider = recipientsProvider;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(final String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(final TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(events).
                append(abouts).
                append(recipientsFIQL).
                append(staticRecipients).
                append(recipientAttrName).
                append(selfAsRecipient).
                append(recipientsProvider).
                append(sender).
                append(subject).
                append(template).
                append(traceLevel).
                append(active).
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
        final NotificationTO other = (NotificationTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(events, other.events).
                append(abouts, other.abouts).
                append(recipientsFIQL, other.recipientsFIQL).
                append(staticRecipients, other.staticRecipients).
                append(recipientAttrName, other.recipientAttrName).
                append(selfAsRecipient, other.selfAsRecipient).
                append(recipientsProvider, other.recipientsProvider).
                append(sender, other.sender).
                append(subject, other.subject).
                append(template, other.template).
                append(traceLevel, other.traceLevel).
                append(active, other.active).
                build();
    }
}
