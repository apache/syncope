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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class", "key", "description" })
@Schema(subTypes = { OIDCRPClientAppTO.class, SAML2SPClientAppTO.class, CASSPClientAppTO.class }, 
        discriminatorProperty = "_class")
public abstract class ClientAppTO implements NamedEntityTO {

    private static final long serialVersionUID = 6577639976115661357L;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    private String discriminator;

    private String key;

    private String name;

    private Long clientAppId;

    private String description;

    private String authPolicy;

    private String accessPolicy;

    private String attrReleasePolicy;

    private String theme;

    private final List<Attr> properties = new ArrayList<>();

    @Schema(name = "_class", required = true)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    public String getAttrReleasePolicy() {
        return attrReleasePolicy;
    }

    public void setAttrReleasePolicy(final String attrReleasePolicy) {
        this.attrReleasePolicy = attrReleasePolicy;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(final String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getAuthPolicy() {
        return authPolicy;
    }

    public void setAuthPolicy(final String authPolicy) {
        this.authPolicy = authPolicy;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(final String theme) {
        this.theme = theme;
    }

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public Long getClientAppId() {
        return clientAppId;
    }

    public void setClientAppId(final Long clientAppId) {
        this.clientAppId = clientAppId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @JacksonXmlElementWrapper(localName = "properties")
    @JacksonXmlProperty(localName = "property")
    public List<Attr> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(key)
                .append(clientAppId)
                .append(name)
                .append(description)
                .append(authPolicy)
                .append(accessPolicy)
                .append(attrReleasePolicy)
                .append(theme)
                .append(properties)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ClientAppTO rhs = (ClientAppTO) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.key, rhs.key)
                .append(this.clientAppId, rhs.clientAppId)
                .append(this.name, rhs.name)
                .append(this.description, rhs.description)
                .append(this.authPolicy, rhs.authPolicy)
                .append(this.accessPolicy, rhs.accessPolicy)
                .append(this.attrReleasePolicy, rhs.attrReleasePolicy)
                .append(this.theme, rhs.theme)
                .append(this.properties, rhs.properties)
                .isEquals();
    }
}
