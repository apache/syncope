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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.EntityTO;

@XmlRootElement(name = "policy")
@XmlType
@XmlSeeAlso({ AccountPolicyTO.class, PasswordPolicyTO.class, ProvisioningPolicyTO.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@class")
@JsonPropertyOrder(value = { "@class", "key", "description" })
@Schema(
        subTypes = { AccountPolicyTO.class, PasswordPolicyTO.class, PullPolicyTO.class },
        discriminatorProperty = "@class")
public abstract class PolicyTO implements EntityTO {

    private static final long serialVersionUID = -2903888572649721035L;

    @XmlTransient
    @JsonProperty("@class")
    private String discriminator;

    private String key;

    private String description;

    private final List<String> usedByResources = new ArrayList<>();

    private final List<String> usedByRealms = new ArrayList<>();

    @Schema(name = "@class", required = true)
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
    @XmlElement(required = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @XmlElementWrapper(name = "usedByResources")
    @XmlElement(name = "resource")
    @JsonProperty("usedByResources")
    public List<String> getUsedByResources() {
        return usedByResources;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @XmlElementWrapper(name = "usedByRealms")
    @XmlElement(name = "group")
    @JsonProperty("usedByRealms")
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
                append(description, policyTO.description).
                append(usedByResources, policyTO.usedByResources).
                append(usedByRealms, policyTO.usedByRealms).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(discriminator).
                append(key).
                append(description).
                append(usedByResources).
                append(usedByRealms).
                build();
    }
}
