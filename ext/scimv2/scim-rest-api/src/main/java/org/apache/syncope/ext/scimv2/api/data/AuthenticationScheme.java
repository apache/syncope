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
import java.net.URI;

public class AuthenticationScheme extends SCIMBean {

    private static final long serialVersionUID = -1326661422976856869L;

    private final String name;

    private final String description;

    private final URI specUri;

    private final URI documentationUri;

    private final String type;

    private final boolean primary;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AuthenticationScheme(
            @JsonProperty("name") final String name,
            @JsonProperty("description") final String description,
            @JsonProperty("specUri") final URI specUri,
            @JsonProperty("documentationUri") final URI documentationUri,
            @JsonProperty("type") final String type,
            @JsonProperty("primary") final boolean primary) {

        this.name = name;
        this.description = description;
        this.specUri = specUri;
        this.documentationUri = documentationUri;
        this.type = type;
        this.primary = primary;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public URI getSpecUri() {
        return specUri;
    }

    public URI getDocumentationUri() {
        return documentationUri;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimary() {
        return primary;
    }
}
