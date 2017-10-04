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
package org.apache.syncope.common.lib.patch;

import java.util.Collection;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.StatusPatchType;

@XmlRootElement(name = "statusPatch")
@XmlType
public class StatusPatch extends PasswordPatch {

    private static final long serialVersionUID = 99309988426922612L;

    public static class Builder extends PasswordPatch.Builder {

        @Override
        protected StatusPatch newInstance() {
            return new StatusPatch();
        }

        @Override
        protected StatusPatch getInstance() {
            return (StatusPatch) super.getInstance();
        }

        @Override
        public StatusPatch build() {
            return (StatusPatch) super.build();
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

        public Builder key(final String key) {
            getInstance().setKey(key);
            return this;
        }

        public Builder type(final StatusPatchType type) {
            getInstance().setType(type);
            return this;
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

    private StatusPatchType type;

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

    public StatusPatchType getType() {
        return type;
    }

    public void setType(final StatusPatchType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

}
