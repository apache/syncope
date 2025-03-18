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
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.types.LogoutType;

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

    private String realm;

    private String name;

    private Long clientAppId;

    private int evaluationOrder;

    private String description;

    private String logo;

    private String theme;

    private String informationUrl;

    private String privacyUrl;

    private UsernameAttributeProviderConf usernameAttributeProviderConf;

    private String authPolicy;

    private String accessPolicy;

    private String attrReleasePolicy;

    private String ticketExpirationPolicy;

    private final List<Attr> properties = new ArrayList<>();

    private LogoutType logoutType = LogoutType.NONE;

    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
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

    public int getEvaluationOrder() {
        return evaluationOrder;
    }

    public void setEvaluationOrder(final int evaluationOrder) {
        this.evaluationOrder = evaluationOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(final String logo) {
        this.logo = logo;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(final String theme) {
        this.theme = theme;
    }

    public String getInformationUrl() {
        return informationUrl;
    }

    public void setInformationUrl(final String informationUrl) {
        this.informationUrl = informationUrl;
    }

    public String getPrivacyUrl() {
        return privacyUrl;
    }

    public void setPrivacyUrl(final String privacyUrl) {
        this.privacyUrl = privacyUrl;
    }

    public UsernameAttributeProviderConf getUsernameAttributeProviderConf() {
        return usernameAttributeProviderConf;
    }

    public void setUsernameAttributeProviderConf(final UsernameAttributeProviderConf usernameAttributeProviderConf) {
        this.usernameAttributeProviderConf = usernameAttributeProviderConf;
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

    public String getTicketExpirationPolicy() {
        return ticketExpirationPolicy;
    }

    public void setTicketExpirationPolicy(final String ticketExpirationPolicy) {
        this.ticketExpirationPolicy = ticketExpirationPolicy;
    }

    @JacksonXmlElementWrapper(localName = "properties")
    @JacksonXmlProperty(localName = "property")
    public List<Attr> getProperties() {
        return properties;
    }

    public LogoutType getLogoutType() {
        return logoutType;
    }

    public void setLogoutType(final LogoutType logoutType) {
        this.logoutType = logoutType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(key)
                .append(realm)
                .append(clientAppId)
                .append(evaluationOrder)
                .append(name)
                .append(description)
                .append(logo)
                .append(theme)
                .append(informationUrl)
                .append(privacyUrl)
                .append(usernameAttributeProviderConf)
                .append(authPolicy)
                .append(accessPolicy)
                .append(attrReleasePolicy)
                .append(ticketExpirationPolicy)
                .append(properties)
                .append(logoutType)
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
                .append(this.realm, rhs.realm)
                .append(this.clientAppId, rhs.clientAppId)
                .append(this.evaluationOrder, rhs.evaluationOrder)
                .append(this.name, rhs.name)
                .append(this.description, rhs.description)
                .append(this.logo, rhs.logo)
                .append(this.theme, rhs.theme)
                .append(this.informationUrl, rhs.informationUrl)
                .append(this.privacyUrl, rhs.privacyUrl)
                .append(this.usernameAttributeProviderConf, rhs.usernameAttributeProviderConf)
                .append(this.authPolicy, rhs.authPolicy)
                .append(this.accessPolicy, rhs.accessPolicy)
                .append(this.attrReleasePolicy, rhs.attrReleasePolicy)
                .append(this.ticketExpirationPolicy, rhs.ticketExpirationPolicy)
                .append(this.properties, rhs.properties)
                .append(this.logoutType, rhs.logoutType)
                .isEquals();
    }
}
