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
package org.apache.syncope.common.lib.to.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import org.apache.syncope.common.lib.types.OIDCSubjectType;

@XmlRootElement(name = "oidcrp")
@XmlType
@Schema(allOf = { ClientAppTO.class })
public class OIDCRPTO extends ClientAppTO {

    private static final long serialVersionUID = -6370888503924521351L;

    private String clientId;

    private String clientSecret;

    private boolean signIdToken;

    private String jwks;

    private OIDCSubjectType subjectType;

    private final List<String> redirectUris = new ArrayList<>();

    private final Set<String> supportedGrantTypes = new HashSet<>();

    private final Set<String> supportedResponseTypes = new HashSet<>();

    @XmlTransient
    @JsonProperty("@class")
    @Schema(name = "@class", required = true,
            example = "org.apache.syncope.common.lib.to.client.OIDCRPTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @XmlElementWrapper(name = "redirectUris")
    @XmlElement(name = "redirectUri")
    @JsonProperty("redirectUris")
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    @XmlElementWrapper(name = "supportedGrantTypes")
    @XmlElement(name = "supportedGrantType")
    @JsonProperty("supportedGrantTypes")
    public Set<String> getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    @XmlElementWrapper(name = "supportedResponseTypes")
    @XmlElement(name = "supportedResponseType")
    @JsonProperty("supportedResponseTypes")
    public Set<String> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    public boolean isSignIdToken() {
        return signIdToken;
    }

    public void setSignIdToken(final boolean signIdToken) {
        this.signIdToken = signIdToken;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(final String jwks) {
        this.jwks = jwks;
    }

    public OIDCSubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(final OIDCSubjectType subjectType) {
        this.subjectType = subjectType;
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
        OIDCRPTO rhs = (OIDCRPTO) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.clientId, rhs.clientId)
                .append(this.clientSecret, rhs.clientSecret)
                .append(this.redirectUris, rhs.redirectUris)
                .append(this.supportedGrantTypes, rhs.supportedGrantTypes)
                .append(this.supportedResponseTypes, rhs.supportedResponseTypes)
                .append(this.signIdToken, rhs.signIdToken)
                .append(this.jwks, rhs.jwks)
                .append(this.subjectType, rhs.subjectType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(clientId)
                .append(clientSecret)
                .append(redirectUris)
                .append(supportedGrantTypes)
                .append(supportedResponseTypes)
                .append(signIdToken)
                .append(jwks)
                .append(subjectType)
                .toHashCode();
    }
}
