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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({ "schemas", "id", "externalId", "displayName", "extensionInfo", "meta" })
public class SCIMAnyObject extends SCIMResource {

    @JsonIgnore
    private String extensionUrn;

    @JsonIgnore
    private SCIMExtensionInfo extensionInfo;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SCIMAnyObject(
            @JsonProperty("id") final String id,
            @JsonProperty("schemas") final List<String> schemas,
            @JsonProperty("meta") final Meta meta,
            @JsonProperty("displayName") final String displayName) {

        super(id, schemas, meta);
        super.setDisplayName(displayName);
        this.extensionUrn = schemas.isEmpty() ? null : schemas.getFirst();
    }

    @JsonAnyGetter
    public Map<String, SCIMExtensionInfo> getExtensionAsMap() {
        if (extensionUrn != null && extensionInfo != null && !extensionInfo.isEmpty()) {
            return Map.of(extensionUrn, extensionInfo);
        }
        return Map.of();
    }

    @JsonAnySetter
    public void setDynamicExtension(final String key, final Object value) {
        if (key.startsWith("urn:ietf:params:scim:schemas:extension:syncope:2.0:")) {
            this.extensionUrn = key;

            if (value instanceof Map) {
                SCIMExtensionInfo info = new SCIMExtensionInfo();
                Map<?, ?> rawMap = (Map<?, ?>) value;
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    info.add(entry.getKey().toString(), entry.getValue().toString());
                }
                this.extensionInfo = info;
            }
        }
    }

    public String getExtensionUrn() {
        return extensionUrn;
    }

    public void setExtensionUrn(final String extensionUrn) {
        this.extensionUrn = extensionUrn;
    }

    public SCIMExtensionInfo getExtensionInfo() {
        return extensionInfo;
    }

    public void setExtensionInfo(final SCIMExtensionInfo extensionInfo) {
        this.extensionInfo = extensionInfo;
    }
}
