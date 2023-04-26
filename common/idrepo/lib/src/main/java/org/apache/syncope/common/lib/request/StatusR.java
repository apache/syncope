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
package org.apache.syncope.common.lib.request;

import jakarta.ws.rs.PathParam;
import java.util.Collection;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.StatusRType;

public class StatusR extends PasswordPatch {

    private static final long serialVersionUID = 99309988426922612L;

    public static class Builder extends PasswordPatch.Builder {

        public Builder(final String key, final StatusRType type) {
            getInstance().setKey(key);
            getInstance().setType(type);
        }

        @Override
        protected StatusR newInstance() {
            return new StatusR();
        }

        @Override
        protected StatusR getInstance() {
            return (StatusR) super.getInstance();
        }

        @Override
        public StatusR build() {
            return (StatusR) super.build();
        }

        @Override
        public Builder onSyncope(final boolean onSyncope) {
            return (Builder) super.onSyncope(onSyncope);
        }

        @Override
        public Builder resource(final String resource) {
            return (Builder) super.resource(resource);
        }

        @Override
        public Builder resources(final Collection<String> resources) {
            return (Builder) super.resources(resources);
        }

        @Override
        public Builder resources(final String... resources) {
            return (Builder) super.resources(resources);
        }

        public Builder token(final String token) {
            getInstance().setToken(token);
            return this;
        }
    }

    /**
     * Key of user to for which status update is requested.
     */
    private String key;

    private StatusRType type;

    /**
     * Update token (if required).
     */
    private String token;

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public StatusRType getType() {
        return type;
    }

    public void setType(final StatusRType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(type).
                append(token).
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
        final StatusR other = (StatusR) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(type, other.type).
                append(token, other.token).
                build();
    }
}
