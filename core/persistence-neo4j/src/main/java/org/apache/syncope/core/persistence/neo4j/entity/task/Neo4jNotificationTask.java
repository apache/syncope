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
package org.apache.syncope.core.persistence.neo4j.entity.task;

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
import org.apache.syncope.core.persistence.neo4j.converters.StringSetConverter;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jNotification;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jNotificationTask.NODE)
public class Neo4jNotificationTask extends AbstractTask<NotificationTask> implements NotificationTask {

    private static final long serialVersionUID = 95731573485279180L;

    public static final String NODE = "NotificationTask";

    public static final String NOTIFICATION_TASK_EXEC_REL = "NOTIFICATION_TASK_EXEC";

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING, cascadeUpdates = false)
    private Neo4jNotification notification;

    private AnyTypeKind anyTypeKind;

    private String entityKey;

    @ConvertWith(converter = StringSetConverter.class)
    private Set<String> recipients = new HashSet<>();

    @Relationship(type = NOTIFICATION_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jNotificationTaskExec> notificationTaskExecs = new ArrayList<>();

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    private String textBody;

    @NotNull
    private String htmlBody;

    @NotNull
    private Boolean executed = false;

    @NotNull
    private TraceLevel traceLevel;

    @Override
    public Notification getNotification() {
        return notification;
    }

    @Override
    public void setNotification(final Notification notification) {
        checkType(notification, Neo4jNotification.class);
        this.notification = (Neo4jNotification) notification;
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
    protected boolean doAdd(final TaskExec<NotificationTask> exec) {
        return notificationTaskExecs.add((Neo4jNotificationTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<NotificationTask>> executionClass() {
        return Neo4jNotificationTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<NotificationTask>> executions() {
        return notificationTaskExecs;
    }
}
