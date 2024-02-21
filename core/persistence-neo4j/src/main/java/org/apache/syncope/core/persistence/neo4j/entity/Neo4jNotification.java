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
package org.apache.syncope.core.persistence.neo4j.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jNotification.NODE)
public class Neo4jNotification extends AbstractGeneratedKeyNode implements Notification {

    private static final long serialVersionUID = 3112582296912757537L;

    public static final String NODE = "Notification";

    public static final String NOTIFICATION_ABOUT_REL = "NOTIFICATION_ABOUT";

    protected static final TypeReference<List<String>> TYPEREF = new TypeReference<List<String>>() {
    };

    private String events;

    @Transient
    private List<String> eventsList = new ArrayList<>();

    @Relationship(type = NOTIFICATION_ABOUT_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jAnyAbout> abouts = new ArrayList<>();

    private String recipientsFIQL;

    private String staticRecipients;

    @Transient
    private List<String> staticRecipientsList = new ArrayList<>();

    @NotNull
    private String recipientAttrName;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation recipientsProvider;

    @NotNull
    private Boolean selfAsRecipient = false;

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jMailTemplate template;

    @NotNull
    private TraceLevel traceLevel = TraceLevel.ALL;

    @NotNull
    private Boolean active = true;

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
    public Implementation getRecipientsProvider() {
        return recipientsProvider;
    }

    @Override
    public void setRecipientsProvider(final Implementation recipientsProvider) {
        checkType(recipientsProvider, Neo4jImplementation.class);
        checkImplementationType(recipientsProvider, IdRepoImplementationType.RECIPIENTS_PROVIDER);
        this.recipientsProvider = (Neo4jImplementation) recipientsProvider;
    }

    @Override
    public List<String> getEvents() {
        return eventsList;
    }

    @Override
    public boolean add(final AnyAbout about) {
        checkType(about, Neo4jAnyAbout.class);
        return this.abouts.add((Neo4jAnyAbout) about);
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
        return staticRecipientsList;
    }

    @Override
    public boolean isSelfAsRecipient() {
        return selfAsRecipient;
    }

    @Override
    public void setSelfAsRecipient(final boolean selfAsRecipient) {
        this.selfAsRecipient = selfAsRecipient;
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
        checkType(template, Neo4jMailTemplate.class);
        this.template = (Neo4jMailTemplate) template;
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
        return active;
    }

    @Override
    public void setActive(final boolean active) {
        this.active = active;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getEvents().clear();
            getStaticRecipients().clear();
        }
        if (events != null) {
            getEvents().addAll(POJOHelper.deserialize(events, TYPEREF));
        }
        if (staticRecipients != null) {
            getStaticRecipients().addAll(POJOHelper.deserialize(staticRecipients, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        events = POJOHelper.serialize(getEvents());
        staticRecipients = POJOHelper.serialize(getStaticRecipients());
    }
}
