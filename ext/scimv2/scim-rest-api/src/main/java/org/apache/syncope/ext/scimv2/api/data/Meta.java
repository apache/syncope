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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.syncope.ext.scimv2.api.type.Resource;

public class Meta extends SCIMBean {

    private static final long serialVersionUID = 8976451652101091915L;

    private final Resource resourceType;

    private final OffsetDateTime created;

    private final OffsetDateTime lastModified;

    @JsonIgnore
    private final EntityTag version;

    private final String location;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Meta(
            @JsonProperty("resourceType") final Resource resourceType,
            @JsonProperty("created") final OffsetDateTime created,
            @JsonProperty("lastModified") final OffsetDateTime lastModified,
            @JsonProperty("version") final String version,
            @JsonProperty("location") final String location) {

        this.resourceType = resourceType;
        this.created = created;
        this.lastModified = lastModified;
        this.version = Optional.ofNullable(version).map(s -> new EntityTag(s, true)).orElse(null);
        this.location = location;
    }

    public Resource getResourceType() {
        return resourceType;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public OffsetDateTime getLastModified() {
        return lastModified;
    }

    @JsonProperty
    public String getVersion() {
        return Optional.ofNullable(version).
                map(value -> RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class).toString(value)).
                orElse(null);
    }

    public String getLocation() {
        return location;
    }
}
