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
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TraceLevel;

@XmlRootElement(name = "notificationTask")
@XmlType
public class NotificationTaskTO extends AbstractTaskTO {

    private static final long serialVersionUID = 371671242591093846L;

    private String notification;

    private AnyTypeKind anyTypeKind;

    private String entityKey;

    private final Set<String> recipients = new HashSet<>();

    private String sender;

    private String subject;

    private String textBody;

    private String htmlBody;

    private boolean executed;

    private TraceLevel traceLevel;

    public String getNotification() {
        return notification;
    }

    public void setNotification(final String notification) {
        this.notification = notification;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @XmlElementWrapper(name = "recipients")
    @XmlElement(name = "recipient")
    @JsonProperty("recipients")
    public Set<String> getRecipients() {
        return recipients;
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

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(final String textBody) {
        this.textBody = textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(final String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(final boolean executed) {
        this.executed = executed;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(final TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }
}
