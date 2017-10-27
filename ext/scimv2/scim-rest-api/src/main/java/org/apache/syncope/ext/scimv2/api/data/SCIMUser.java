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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({ "schemas", "id", "externalId", "userName", "active", "groups", "roles", "meta" })
public class SCIMUser extends SCIMResource {

    private static final long serialVersionUID = -2935466041674390279L;

    private final String userName;

    private final boolean active;

    private final List<Group> groups = new ArrayList<>();

    private final List<Display> roles = new ArrayList<>();

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SCIMUser(
            @JsonProperty("id") final String id,
            @JsonProperty("schemas") final List<String> schemas,
            @JsonProperty("meta") final Meta meta,
            @JsonProperty("userName") final String userName,
            @JsonProperty("active") final boolean active) {

        super(id, schemas, meta);
        this.userName = userName;
        this.active = active;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isActive() {
        return active;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Display> getRoles() {
        return roles;
    }

}
