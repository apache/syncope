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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Notification;

@Entity
@Table(name = JPAAnyAbout.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "notification_id", "anyType_id" }))
public class JPAAnyAbout extends AbstractGeneratedKeyEntity implements AnyAbout {

    private static final long serialVersionUID = 3517381731849788407L;

    public static final String TABLE = "AnyAbout";

    @ManyToOne
    private JPANotification notification;

    @ManyToOne
    private JPAAnyType anyType;

    @Column(name = "anyType_filter")
    @Lob
    private String filter;

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
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
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
