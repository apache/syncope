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

package org.apache.syncope.common.lib.to;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.Date;

@XmlRootElement(name = "gauthToken")
@XmlType
public class GoogleMfaAuthTokenTO extends BaseBean implements EntityTO {

    private static final long serialVersionUID = 1285073386484048953L;

    private String key;

    private Integer token;

    private String user;

    private Date issuedDate;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public Integer getToken() {
        return token;
    }

    public void setToken(final Integer token) {
        this.token = token;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(final Date issuedDate) {
        this.issuedDate = issuedDate;
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
        GoogleMfaAuthTokenTO rhs = (GoogleMfaAuthTokenTO) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(this.key, rhs.key)
            .append(this.token, rhs.token)
            .append(this.user, rhs.user)
            .append(this.issuedDate, rhs.issuedDate)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(key)
            .append(token)
            .append(user)
            .append(issuedDate)
            .toHashCode();
    }

    public static class Builder {

        private final GoogleMfaAuthTokenTO instance = new GoogleMfaAuthTokenTO();


        public GoogleMfaAuthTokenTO.Builder issuedDate(final Date issued) {
            instance.setIssuedDate(issued);
            return this;
        }

        public GoogleMfaAuthTokenTO.Builder token(final Integer token) {
            instance.setToken(token);
            return this;
        }

        public GoogleMfaAuthTokenTO.Builder user(final String user) {
            instance.setUser(user);
            return this;
        }

        public GoogleMfaAuthTokenTO.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public GoogleMfaAuthTokenTO build() {
            return instance;
        }
    }
}
