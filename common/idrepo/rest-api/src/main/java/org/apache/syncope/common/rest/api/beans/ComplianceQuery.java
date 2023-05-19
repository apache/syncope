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
package org.apache.syncope.common.rest.api.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ComplianceQuery implements Serializable {

    private static final long serialVersionUID = -7324275079761880426L;

    public static class Builder {

        private final ComplianceQuery instance = new ComplianceQuery();

        public Builder username(final String username) {
            instance.setUsername(username);
            return this;
        }

        public Builder password(final String password) {
            instance.setPassword(password);
            return this;
        }

        public Builder realm(final String realm) {
            instance.setRealm(realm);
            return this;
        }

        public ComplianceQuery build() {
            return instance;
        }

        public Builder resource(final String resource) {
            if (resource != null) {
                instance.getResources().add(resource);
            }
            return this;
        }

        public Builder resources(final String... resources) {
            instance.getResources().addAll(List.of(resources));
            return this;
        }

        public Builder resources(final Collection<String> resources) {
            if (resources != null) {
                instance.getResources().addAll(resources);
            }
            return this;
        }
    }

    private String username;

    private String password;

    private String realm;

    private Set<String> resources = new HashSet<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public Set<String> getResources() {
        return resources;
    }

    public void setResources(final Set<String> resources) {
        this.resources = resources;
    }

    @JsonIgnore
    public boolean isEmpty() {
        if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
            return true;
        }
        return StringUtils.isEmpty(realm) && resources.isEmpty();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ComplianceQuery other = (ComplianceQuery) obj;
        return new EqualsBuilder().
                append(username, other.username).
                append(password, other.password).
                append(realm, other.realm).
                append(resources, other.resources).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(username).
                append(password).
                append(realm).
                append(resources).
                build();
    }
}
