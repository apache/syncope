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

@JsonPropertyOrder({ "schemas", "id", "externalId", "displayName", "members", "extensionInfo", "meta" })
public class SCIMGroup extends SCIMResource {

    private static final long serialVersionUID = -2935466041674390279L;

    private final List<Member> members = new ArrayList<>();

    @JsonProperty("urn:ietf:params:scim:schemas:extension:syncope:2.0:Group")
    private SCIMExtensionInfo extensionInfo;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SCIMGroup(
            @JsonProperty("id") final String id,
            @JsonProperty("schemas") final List<String> schemas,
            @JsonProperty("meta") final Meta meta,
            @JsonProperty("displayName") final String displayName) {

        super(id, schemas, meta);
        super.setDisplayName(displayName);
    }

    public List<Member> getMembers() {
        return members;
    }

    public SCIMExtensionInfo getExtensionInfo() {
        return extensionInfo;
    }

    public void setExtensionInfo(final SCIMExtensionInfo extensionInfo) {
        this.extensionInfo = extensionInfo;
    }
}
