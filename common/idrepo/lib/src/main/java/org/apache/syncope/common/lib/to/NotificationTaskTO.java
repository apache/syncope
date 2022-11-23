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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TraceLevel;

@Schema(allOf = { TaskTO.class })
public class NotificationTaskTO extends TaskTO {

    private static final long serialVersionUID = 371671242591093846L;

    private String notification;

    private AnyTypeKind anyTypeKind;

    private String entityKey;

    private final Set<String> recipients = new TreeSet<>();

    private String sender;

    private String subject;

    private String textBody;

    private String htmlBody;

    private boolean executed;

    private TraceLevel traceLevel;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.NotificationTaskTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getNotification() {
        return notification;
    }

    public void setNotification(final String notification) {
        this.notification = notification;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JacksonXmlElementWrapper(localName = "recipients")
    @JacksonXmlProperty(localName = "recipient")
    public Set<String> getRecipients() {
        return recipients;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getSender() {
        return sender;
    }

    public void setSender(final String sender) {
        this.sender = sender;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(final String textBody) {
        this.textBody = textBody;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(final String htmlBody) {
        this.htmlBody = htmlBody;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(final boolean executed) {
        this.executed = executed;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(final TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(notification).
                append(anyTypeKind).
                append(entityKey).
                append(recipients).
                append(sender).
                append(subject).
                append(textBody).
                append(htmlBody).
                append(executed).
                append(traceLevel).
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
        final NotificationTaskTO other = (NotificationTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(notification, other.notification).
                append(anyTypeKind, other.anyTypeKind).
                append(entityKey, other.entityKey).
                append(recipients, other.recipients).
                append(sender, other.sender).
                append(subject, other.subject).
                append(textBody, other.textBody).
                append(htmlBody, other.htmlBody).
                append(executed, other.executed).
                append(traceLevel, other.traceLevel).
                build();
    }
}
