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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.OIDCApplicationType;
import org.apache.syncope.common.lib.types.OIDCClientAuthenticationMethod;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionAlg;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;

@Schema(allOf = { ClientAppTO.class })
public class OIDCRPClientAppTO extends ClientAppTO {

    private static final long serialVersionUID = -6370888503924521351L;

    private String clientId;

    private String clientSecret;

    private String idTokenIssuer;

    private boolean signIdToken = true;

    private OIDCTokenSigningAlg idTokenSigningAlg = OIDCTokenSigningAlg.none;

    private boolean encryptIdToken;

    private OIDCTokenEncryptionAlg idTokenEncryptionAlg = OIDCTokenEncryptionAlg.none;

    private OIDCTokenEncryptionEncoding idTokenEncryptionEncoding;

    private OIDCTokenSigningAlg userInfoSigningAlg;

    private OIDCTokenEncryptionAlg userInfoEncryptedResponseAlg;

    private OIDCTokenEncryptionEncoding userInfoEncryptedResponseEncoding;

    private boolean jwtAccessToken;

    private boolean bypassApprovalPrompt = true;

    private boolean generateRefreshToken = true;

    private OIDCSubjectType subjectType = OIDCSubjectType.PUBLIC;

    private OIDCApplicationType applicationType = OIDCApplicationType.WEB;

    private final List<String> redirectUris = new ArrayList<>();

    private final List<OIDCGrantType> supportedGrantTypes = new ArrayList<>();

    private final List<OIDCResponseType> supportedResponseTypes = new ArrayList<>();

    private final List<String> scopes = new ArrayList<>();

    private String jwks;

    private String jwksUri;

    private OIDCClientAuthenticationMethod tokenEndpointAuthenticationMethod =
            OIDCClientAuthenticationMethod.client_secret_basic;

    private String logoutUri;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
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

    @JacksonXmlElementWrapper(localName = "redirectUris")
    @JacksonXmlProperty(localName = "redirectUri")
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    @JacksonXmlElementWrapper(localName = "supportedGrantTypes")
    @JacksonXmlProperty(localName = "supportedGrantType")
    public List<OIDCGrantType> getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    @JacksonXmlElementWrapper(localName = "supportedResponseTypes")
    @JacksonXmlProperty(localName = "supportedResponseType")
    public List<OIDCResponseType> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    public String getIdTokenIssuer() {
        return idTokenIssuer;
    }

    public void setIdTokenIssuer(final String idTokenIssuer) {
        this.idTokenIssuer = idTokenIssuer;
    }

    public boolean isSignIdToken() {
        return signIdToken;
    }

    public void setSignIdToken(final boolean signIdToken) {
        this.signIdToken = signIdToken;
    }

    public boolean isEncryptIdToken() {
        return encryptIdToken;
    }

    public void setEncryptIdToken(final boolean encryptIdToken) {
        this.encryptIdToken = encryptIdToken;
    }

    public OIDCTokenSigningAlg getIdTokenSigningAlg() {
        return idTokenSigningAlg;
    }

    public void setIdTokenSigningAlg(final OIDCTokenSigningAlg idTokenSigningAlg) {
        this.idTokenSigningAlg = idTokenSigningAlg;
    }

    public OIDCTokenEncryptionAlg getIdTokenEncryptionAlg() {
        return idTokenEncryptionAlg;
    }

    public void setIdTokenEncryptionAlg(final OIDCTokenEncryptionAlg idTokenEncryptionAlg) {
        this.idTokenEncryptionAlg = idTokenEncryptionAlg;
    }

    public OIDCTokenEncryptionEncoding getIdTokenEncryptionEncoding() {
        return idTokenEncryptionEncoding;
    }

    public void setIdTokenEncryptionEncoding(final OIDCTokenEncryptionEncoding idTokenEncryptionEncoding) {
        this.idTokenEncryptionEncoding = idTokenEncryptionEncoding;
    }

    public OIDCTokenSigningAlg getUserInfoSigningAlg() {
        return userInfoSigningAlg;
    }

    public void setUserInfoSigningAlg(final OIDCTokenSigningAlg userInfoSigningAlg) {
        this.userInfoSigningAlg = userInfoSigningAlg;
    }

    public OIDCTokenEncryptionAlg getUserInfoEncryptedResponseAlg() {
        return userInfoEncryptedResponseAlg;
    }

    public void setUserInfoEncryptedResponseAlg(final OIDCTokenEncryptionAlg userInfoEncryptedResponseAlg) {
        this.userInfoEncryptedResponseAlg = userInfoEncryptedResponseAlg;
    }

    public OIDCTokenEncryptionEncoding getUserInfoEncryptedResponseEncoding() {
        return userInfoEncryptedResponseEncoding;
    }

    public void setUserInfoEncryptedResponseEncoding(final OIDCTokenEncryptionEncoding encoding) {
        this.userInfoEncryptedResponseEncoding = encoding;
    }

    public OIDCSubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(final OIDCSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public OIDCApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(final OIDCApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    public boolean isJwtAccessToken() {
        return jwtAccessToken;
    }

    public void setJwtAccessToken(final boolean jwtAccessToken) {
        this.jwtAccessToken = jwtAccessToken;
    }

    @JacksonXmlElementWrapper(localName = "scopes")
    @JacksonXmlProperty(localName = "scope")
    public List<String> getScopes() {
        return scopes;
    }

    public boolean isBypassApprovalPrompt() {
        return bypassApprovalPrompt;
    }

    public void setBypassApprovalPrompt(final boolean bypassApprovalPrompt) {
        this.bypassApprovalPrompt = bypassApprovalPrompt;
    }

    public boolean isGenerateRefreshToken() {
        return generateRefreshToken;
    }

    public void setGenerateRefreshToken(final boolean generateRefreshToken) {
        this.generateRefreshToken = generateRefreshToken;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(final String jwks) {
        this.jwks = jwks;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public OIDCClientAuthenticationMethod getTokenEndpointAuthenticationMethod() {
        return tokenEndpointAuthenticationMethod;
    }

    public void setTokenEndpointAuthenticationMethod(
            final OIDCClientAuthenticationMethod tokenEndpointAuthenticationMethod) {
        this.tokenEndpointAuthenticationMethod = tokenEndpointAuthenticationMethod;
    }

    public String getLogoutUri() {
        return logoutUri;
    }

    public void setLogoutUri(final String logoutUri) {
        this.logoutUri = logoutUri;
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
        OIDCRPClientAppTO rhs = (OIDCRPClientAppTO) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.clientId, rhs.clientId)
                .append(this.clientSecret, rhs.clientSecret)
                .append(this.idTokenIssuer, rhs.idTokenIssuer)
                .append(this.signIdToken, rhs.signIdToken)
                .append(this.idTokenSigningAlg, rhs.idTokenSigningAlg)
                .append(this.encryptIdToken, rhs.encryptIdToken)
                .append(this.idTokenEncryptionAlg, rhs.idTokenEncryptionAlg)
                .append(this.idTokenEncryptionEncoding, rhs.idTokenEncryptionEncoding)
                .append(this.userInfoSigningAlg, rhs.userInfoSigningAlg)
                .append(this.userInfoEncryptedResponseAlg, rhs.userInfoEncryptedResponseAlg)
                .append(this.userInfoEncryptedResponseEncoding, rhs.userInfoEncryptedResponseEncoding)
                .append(this.jwtAccessToken, rhs.jwtAccessToken)
                .append(this.bypassApprovalPrompt, rhs.bypassApprovalPrompt)
                .append(this.generateRefreshToken, rhs.generateRefreshToken)
                .append(this.subjectType, rhs.subjectType)
                .append(this.applicationType, rhs.applicationType)
                .append(this.redirectUris, rhs.redirectUris)
                .append(this.supportedGrantTypes, rhs.supportedGrantTypes)
                .append(this.supportedResponseTypes, rhs.supportedResponseTypes)
                .append(this.scopes, rhs.scopes)
                .append(this.jwks, rhs.jwks)
                .append(this.jwksUri, rhs.jwksUri)
                .append(this.tokenEndpointAuthenticationMethod, rhs.tokenEndpointAuthenticationMethod)
                .append(this.logoutUri, rhs.logoutUri)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(clientId)
                .append(clientSecret)
                .append(idTokenIssuer)
                .append(signIdToken)
                .append(idTokenSigningAlg)
                .append(encryptIdToken)
                .append(idTokenEncryptionAlg)
                .append(idTokenEncryptionEncoding)
                .append(userInfoSigningAlg)
                .append(userInfoEncryptedResponseAlg)
                .append(userInfoEncryptedResponseEncoding)
                .append(jwtAccessToken)
                .append(bypassApprovalPrompt)
                .append(generateRefreshToken)
                .append(subjectType)
                .append(applicationType)
                .append(redirectUris)
                .append(supportedGrantTypes)
                .append(supportedResponseTypes)
                .append(scopes)
                .append(jwks)
                .append(jwksUri)
                .append(tokenEndpointAuthenticationMethod)
                .append(logoutUri)
                .toHashCode();
    }
}
