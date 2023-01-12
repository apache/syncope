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
import org.apache.syncope.common.lib.types.ResourceAssociationAction;

/**
 * Resource Association Request.
 */
public class ResourceAR extends PasswordPatch {

    private static final long serialVersionUID = 6295778399633883767L;

    public static class Builder extends PasswordPatch.Builder {

        @Override
        protected ResourceAR newInstance() {
            return new ResourceAR();
        }

        @Override
        protected ResourceAR getInstance() {
            return (ResourceAR) super.getInstance();
        }

        @Override
        public ResourceAR build() {
            return (ResourceAR) super.build();
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

        @Override
        public Builder value(final String value) {
            return (Builder) super.value(value);
        }

        public Builder key(final String key) {
            getInstance().setKey(key);
            return this;
        }

        public Builder action(final ResourceAssociationAction action) {
            getInstance().setAction(action);
            return this;
        }
    }

    private String key;

    private ResourceAssociationAction action;

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public ResourceAssociationAction getAction() {
        return action;
    }

    @PathParam("action")
    public void setAction(final ResourceAssociationAction action) {
        this.action = action;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(action).
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
        final ResourceAR other = (ResourceAR) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(action, other.action).
                build();
    }
}
