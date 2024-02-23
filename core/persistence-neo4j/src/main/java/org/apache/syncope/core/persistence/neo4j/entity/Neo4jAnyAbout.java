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

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jAnyAbout.NODE)
public class Neo4jAnyAbout extends AbstractGeneratedKeyNode implements AnyAbout {

    private static final long serialVersionUID = 3517381731849788407L;

    public static final String NODE = "AnyAbout";

    @NotNull
    @Relationship(type = Neo4jNotification.NOTIFICATION_ABOUT_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jNotification notification;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType anyType;

    private String filter;

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
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.anyType = (Neo4jAnyType) anyType;
    }

    @Override
    public String get() {
        return filter;
    }

    @Override
    public void set(final String filter) {
        this.filter = filter;
    }
}
