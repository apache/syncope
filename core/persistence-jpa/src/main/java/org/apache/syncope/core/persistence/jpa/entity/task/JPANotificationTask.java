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
package org.apache.syncope.core.persistence.jpa.entity.task;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.converters.StringSetConverter;
import org.apache.syncope.core.persistence.jpa.entity.JPANotification;

@Entity
@Table(name = JPANotificationTask.TABLE)
public class JPANotificationTask extends AbstractTask<NotificationTask> implements NotificationTask {

    private static final long serialVersionUID = 95731573485279180L;

    public static final String TABLE = "NotificationTask";

    @NotNull
    @ManyToOne
    private JPANotification notification;

    @Enumerated(EnumType.STRING)
    private AnyTypeKind anyTypeKind;

    private String entityKey;

    @Convert(converter = StringSetConverter.class)
    @Lob
    private Set<String> recipients = new HashSet<>();

    @OneToMany(targetEntity = JPANotificationTaskExec.class,
            cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    private List<TaskExec<NotificationTask>> executions = new ArrayList<>();

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    @Lob
    private String textBody;

    @NotNull
    @Lob
    private String htmlBody;

    @NotNull
    private Boolean executed = false;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel traceLevel;

    @Override
    public Notification getNotification() {
        return notification;
    }

    @Override
    public void setNotification(final Notification notification) {
        checkType(notification, JPANotification.class);
        this.notification = (JPANotification) notification;
    }

    @Override
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Override
    public String getEntityKey() {
        return entityKey;
    }

    @Override
    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @Override
    public Set<String> getRecipients() {
        return recipients;
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
    public String getTextBody() {
        return textBody;
    }

    @Override
    public void setTextBody(final String textBody) {
        this.textBody = textBody;
    }

    @Override
    public String getHtmlBody() {
        return htmlBody;
    }

    @Override
    public void setHtmlBody(final String htmlBody) {
        this.htmlBody = htmlBody;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public void setExecuted(final boolean executed) {
        this.executed = executed;
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
    protected Class<? extends TaskExec<NotificationTask>> executionClass() {
        return JPANotificationTaskExec.class;
    }

    @Override
    protected List<TaskExec<NotificationTask>> executions() {
        return executions;
    }
}
