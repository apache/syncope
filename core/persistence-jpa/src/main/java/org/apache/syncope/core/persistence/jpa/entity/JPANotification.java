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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;

@Entity
@Table(name = JPANotification.TABLE)
public class JPANotification extends AbstractGeneratedKeyEntity implements Notification {

    private static final long serialVersionUID = 3112582296912757537L;

    public static final String TABLE = "Notification";

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "event")
    @CollectionTable(name = "Notification_events",
            joinColumns =
            @JoinColumn(name = "notification_id", referencedColumnName = "id"))
    private List<String> events;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "notification")
    private List<JPAAnyAbout> abouts;

    private String recipientsFIQL;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Notification_staticRecipients",
            joinColumns =
            @JoinColumn(name = "notification_id", referencedColumnName = "id"))
    @Column(name = "staticRecipients")
    private List<String> staticRecipients;

    @NotNull
    private String recipientAttrName;

    private String recipientsProviderClassName;

    @NotNull
    @Basic
    @Min(0)
    @Max(1)
    private Integer selfAsRecipient;

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "template_id")
    private JPAMailTemplate template;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TraceLevel traceLevel;

    @NotNull
    @Basic
    @Min(0)
    @Max(1)
    private Integer active;

    public JPANotification() {
        events = new ArrayList<>();
        abouts = new ArrayList<>();
        staticRecipients = new ArrayList<>();
        selfAsRecipient = getBooleanAsInteger(false);
        active = getBooleanAsInteger(true);
        traceLevel = TraceLevel.ALL;
    }

    @Override
    public String getRecipientsFIQL() {
        return recipientsFIQL;
    }

    @Override
    public void setRecipientsFIQL(final String recipientsFIQL) {
        this.recipientsFIQL = recipientsFIQL;
    }

    @Override
    public String getRecipientAttrName() {
        return recipientAttrName;
    }

    @Override
    public void setRecipientAttrName(final String recipientAttrName) {
        this.recipientAttrName = recipientAttrName;
    }

    @Override
    public String getRecipientsProviderClassName() {
        return recipientsProviderClassName;
    }

    @Override
    public void setRecipientsProviderClassName(final String recipientsProviderClassName) {
        this.recipientsProviderClassName = recipientsProviderClassName;
    }

    @Override
    public List<String> getEvents() {
        return events;
    }

    @Override
    public boolean add(final AnyAbout about) {
        checkType(about, JPAAnyAbout.class);
        return this.abouts.add((JPAAnyAbout) about);
    }

    @Override
    public Optional<? extends AnyAbout> getAbout(final AnyType anyType) {
        return abouts.stream().filter(about -> anyType != null && anyType.equals(about.getAnyType())).findFirst();
    }

    @Override
    public List<? extends AnyAbout> getAbouts() {
        return abouts;
    }

    @Override
    public List<String> getStaticRecipients() {
        return staticRecipients;
    }

    @Override
    public boolean isSelfAsRecipient() {
        return isBooleanAsInteger(selfAsRecipient);
    }

    @Override
    public void setSelfAsRecipient(final boolean selfAsRecipient) {
        this.selfAsRecipient = getBooleanAsInteger(selfAsRecipient);
    }

    @Override
    public String getSender() {
        return sender;
    }

    @Override
    public void setSender(final String sender) {
        this.sender = sender;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @Override
    public MailTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final MailTemplate template) {
        checkType(template, JPAMailTemplate.class);
        this.template = (JPAMailTemplate) template;
    }

    @Override
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    @Override
    public void setTraceLevel(final TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    @Override
    public boolean isActive() {
        return isBooleanAsInteger(active);
    }

    @Override
    public void setActive(final boolean active) {
        this.active = getBooleanAsInteger(active);
    }
}
