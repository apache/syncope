/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.common.lib.types;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

@XmlRootElement(name = "googleMfaAuthToken")
@XmlType
public class GoogleMfaAuthToken implements Serializable {
    private static final long serialVersionUID = 2185073386484048953L;

    private String key;

    private Integer token;

    private String owner;

    private Date issueDate;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Integer getToken() {
        return token;
    }

    public void setToken(final Integer token) {
        this.token = token;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public Date getIssueDate() {
        return Optional.ofNullable(this.issueDate).
            map(date -> new Date(date.getTime())).orElse(null);
    }

    public void setIssueDate(final Date issueDate) {
        this.issueDate = Optional.ofNullable(issueDate).
            map(date -> new Date(date.getTime())).orElse(null);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(key)
            .append(token)
            .append(owner)
            .append(issueDate)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        GoogleMfaAuthToken rhs = (GoogleMfaAuthToken) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(this.key, rhs.key)
            .append(this.token, rhs.token)
            .append(this.owner, rhs.owner)
            .append(this.issueDate, rhs.issueDate)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("key", key)
            .append("token", token)
            .append("owner", owner)
            .append("issueDate", issueDate)
            .toString();
    }
    
    public static class Builder {

        private final GoogleMfaAuthToken instance = new GoogleMfaAuthToken();


        public GoogleMfaAuthToken.Builder issueDate(final Date issued) {
            instance.setIssueDate(issued);
            return this;
        }

        public GoogleMfaAuthToken.Builder token(final Integer token) {
            instance.setToken(token);
            return this;
        }

        public GoogleMfaAuthToken.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public GoogleMfaAuthToken.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public GoogleMfaAuthToken build() {
            return instance;
        }
    }
}
