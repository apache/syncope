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
package org.apache.syncope.common.lib.types;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class WebAuthnDeviceCredential implements BaseBean {

    private static final long serialVersionUID = 1185073386484048953L;

    private String json;

    private String owner;

    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public String getJson() {
        return json;
    }

    public void setJson(final String json) {
        this.json = json;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(json)
            .append(identifier)
            .append(owner)
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
        WebAuthnDeviceCredential rhs = (WebAuthnDeviceCredential) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(this.json, rhs.json)
            .append(this.identifier, rhs.identifier)
            .append(this.owner, rhs.owner)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("records", json)
            .append("identifier", identifier)
            .append("owner", owner)
            .toString();
    }

    public static class Builder {

        private final WebAuthnDeviceCredential instance = new WebAuthnDeviceCredential();

        public WebAuthnDeviceCredential.Builder json(final String json) {
            instance.setJson(json);
            return this;
        }

        public WebAuthnDeviceCredential.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public WebAuthnDeviceCredential.Builder identifier(final String identifier) {
            instance.setIdentifier(identifier);
            return this;
        }

        public WebAuthnDeviceCredential build() {
            return instance;
        }
    }
}
