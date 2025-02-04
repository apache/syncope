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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;

public class LinkedAccountTO implements EntityTO {

    private static final long serialVersionUID = 7396929732310559535L;

    public static class Builder {

        private final LinkedAccountTO instance = new LinkedAccountTO();

        public Builder(final String resource, final String connObjectKeyValue) {
            this(null, resource, connObjectKeyValue);
        }

        public Builder(final String key, final String resource, final String connObjectKeyValue) {
            instance.setKey(key);
            instance.setResource(resource);
            instance.setConnObjectKeyValue(connObjectKeyValue);
        }

        public Builder username(final String username) {
            instance.setUsername(username);
            return this;
        }

        public Builder password(final String password) {
            instance.setPassword(password);
            return this;
        }

        public Builder suspended(final boolean suspended) {
            instance.setSuspended(suspended);
            return this;
        }

        public LinkedAccountTO build() {
            return instance;
        }
    }

    private String key;

    private String connObjectKeyValue;

    private String resource;

    private String username;

    private String password;

    private boolean suspended;

    private final Set<Attr> plainAttrs = new TreeSet<>();

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getConnObjectKeyValue() {
        return connObjectKeyValue;
    }

    public void setConnObjectKeyValue(final String connObjectKeyValue) {
        this.connObjectKeyValue = connObjectKeyValue;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

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

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(final boolean suspended) {
        this.suspended = suspended;
    }

    @JacksonXmlElementWrapper(localName = "plainAttrs")
    @JacksonXmlProperty(localName = "plainAttr")
    public Set<Attr> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    public Optional<Attr> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(connObjectKeyValue).
                append(resource).
                append(username).
                append(suspended).
                append(plainAttrs).
                build();
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
        final LinkedAccountTO other = (LinkedAccountTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(connObjectKeyValue, other.connObjectKeyValue).
                append(resource, other.resource).
                append(username, other.username).
                append(suspended, other.suspended).
                append(plainAttrs, other.plainAttrs).
                build();
    }
}
