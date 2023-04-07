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
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.ext.scimv2.api.type.Resource;

public class ResourceType extends SCIMBean {

    private static final long serialVersionUID = -6559584102333757279L;

    private final List<String> schemas = List.of(Resource.ResourceType.schema());

    private final String id;

    private final String name;

    private final String endpoint;

    private final String description;

    private final String schema;

    private final List<SchemaExtension> schemaExtensions = new ArrayList<>();

    private final Meta meta;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ResourceType(
            @JsonProperty("id") final String id,
            @JsonProperty("name") final String name,
            @JsonProperty("endpoint") final String endpoint,
            @JsonProperty("description") final String description,
            @JsonProperty("schema") final String schema,
            @JsonProperty("meta") final Meta meta) {

        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
        this.description = description;
        this.schema = schema;
        this.meta = meta;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getDescription() {
        return description;
    }

    public String getSchema() {
        return schema;
    }

    public List<SchemaExtension> getSchemaExtensions() {
        return schemaExtensions;
    }

    public Meta getMeta() {
        return meta;
    }
}
