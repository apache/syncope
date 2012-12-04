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
package org.apache.syncope.to;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.AbstractBaseBean;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.TraceLevel;

@XmlType
public class NotificationTO extends AbstractBaseBean {

    private static final long serialVersionUID = -6145117115632592612L;

    private Long id;

    private List<String> events;

    private NodeCond about;

    private NodeCond recipients;

    private IntMappingType recipientAttrType;

    private String recipientAttrName;

    private boolean selfAsRecipient;

    private String sender;

    private String subject;

    private String template;

    private TraceLevel traceLevel;

    public NotificationTO() {
        events = new ArrayList<String>();
    }

    public NodeCond getAbout() {
        return about;
    }

    public void setAbout(NodeCond about) {
        this.about = about;
    }

    @XmlElement(name = " event")
    @XmlElementWrapper(name = "events")
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
        this.events = events;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NodeCond getRecipients() {
        return recipients;
    }

    public void setRecipients(NodeCond recipients) {
        this.recipients = recipients;
    }

    public String getRecipientAttrName() {
        return recipientAttrName;
    }

    public void setRecipientAttrName(String recipientAttrName) {
        this.recipientAttrName = recipientAttrName;
    }

    public IntMappingType getRecipientAttrType() {
        return recipientAttrType;
    }

    public void setRecipientAttrType(IntMappingType recipientAttrType) {
        this.recipientAttrType = recipientAttrType;
    }

    public boolean isSelfAsRecipient() {
        return selfAsRecipient;
    }

    public void setSelfAsRecipient(boolean selfAsRecipient) {
        this.selfAsRecipient = selfAsRecipient;
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
