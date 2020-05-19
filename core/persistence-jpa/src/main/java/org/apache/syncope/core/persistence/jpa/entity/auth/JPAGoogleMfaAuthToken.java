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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import org.apache.syncope.core.persistence.api.entity.auth.GoogleMfaAuthToken;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.util.Date;
import java.util.Optional;

@Entity
@Table(name = JPAGoogleMfaAuthToken.TABLE)
public class JPAGoogleMfaAuthToken extends AbstractGeneratedKeyEntity implements GoogleMfaAuthToken {

    public static final String TABLE = "GoogleMfaAuthToken";

    private static final long serialVersionUID = 57352617217394093L;

    @Column(nullable = false)
    private Integer token;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private Date issuedDate;

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final String user) {
        this.owner = user;
    }

    @Override
    public Date getIssuedDate() {
        return Optional.ofNullable(issuedDate).map(date -> new Date(date.getTime())).orElse(null);
    }

    @Override
    public void setIssuedDate(final Date issuedDateTime) {
        this.issuedDate = Optional.ofNullable(issuedDateTime).map(date -> new Date(date.getTime())).orElse(null);
    }

    @Override
    public Integer getToken() {
        return token;
    }

    @Override
    public void setToken(final Integer token) {
        this.token = token;
    }
}
