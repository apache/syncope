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
package org.syncope.core.persistence.beans;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.validation.entity.NotificationCheck;
import org.syncope.client.util.XMLSerializer;
import org.syncope.types.TraceLevel;

@NotificationCheck
@Entity
public class Notification extends AbstractBaseBean {

    private static final long serialVersionUID = 3112582296912757537L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "events")
    private List<String> events;

    @NotNull
    @Lob
    private String xmlAbout;

    @NotNull
    @Lob
    private String xmlRecipients;

    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer selfAsRecipient;

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    private String template;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel traceLevel;

    public Notification() {
        events = new ArrayList<String>();
        selfAsRecipient = getBooleanAsInteger(false);
        traceLevel = TraceLevel.ALL;
    }

    public Long getId() {
        return id;
    }

    public NodeCond getAbout() {
        NodeCond result = XMLSerializer.<NodeCond>deserialize(xmlAbout);
        if (result == null) {
            result = new NodeCond();
        }
        return result;
    }

    public void setAbout(NodeCond about) {
        if (about == null) {
            about = new NodeCond();
        }

        xmlAbout = XMLSerializer.serialize(about);
    }

    public NodeCond getRecipients() {
        NodeCond result = XMLSerializer.<NodeCond>deserialize(xmlRecipients);
        if (result == null) {
            result = new NodeCond();
        }
        return result;
    }

    public void setRecipients(NodeCond recipients) {
        if (recipients == null) {
            recipients = new NodeCond();
        }

        xmlRecipients = XMLSerializer.serialize(recipients);
    }

    public List<String> getEvents() {
        return events;
    }

    public boolean addEvent(final String event) {
        return event != null && !events.contains(event) && events.add(event);
    }

    public boolean removeEvent(final String event) {
        return event != null && events.remove(event);
    }

    public void setEvents(List<String> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
    }

    public boolean isSelfAsRecipient() {
        return isBooleanAsInteger(selfAsRecipient);
    }

    public void setSelfAsRecipient(final boolean selfAsRecipient) {
        this.selfAsRecipient = getBooleanAsInteger(selfAsRecipient);
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }
}
