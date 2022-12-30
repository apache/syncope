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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.NamedEntityTO;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class", "key", "name" })
@Schema(subTypes = { AccountPolicyTO.class, PasswordPolicyTO.class }, discriminatorProperty = "_class")
public abstract class PolicyTO implements NamedEntityTO {

    private static final long serialVersionUID = -2903888572649721035L;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    private String discriminator;

    private String key;

    private String name;

    private final List<String> usedByResources = new ArrayList<>();

    private final List<String> usedByRealms = new ArrayList<>();

    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @JsonProperty(required = true)
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JacksonXmlElementWrapper(localName = "usedByResources")
    @JacksonXmlProperty(localName = "resource")
    public List<String> getUsedByResources() {
        return usedByResources;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JacksonXmlElementWrapper(localName = "usedByRealms")
    @JacksonXmlProperty(localName = "group")
    public List<String> getUsedByRealms() {
        return usedByRealms;
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
        PolicyTO policyTO = (PolicyTO) obj;
        return new EqualsBuilder().
                append(discriminator, policyTO.discriminator).
                append(key, policyTO.key).
                append(name, policyTO.name).
                append(usedByResources, policyTO.usedByResources).
                append(usedByRealms, policyTO.usedByRealms).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(discriminator).
                append(key).
                append(name).
                append(usedByResources).
                append(usedByRealms).
                build();
    }
}
