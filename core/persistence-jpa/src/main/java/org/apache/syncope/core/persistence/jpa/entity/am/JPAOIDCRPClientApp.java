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
package org.apache.syncope.core.persistence.jpa.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.OIDCApplicationType;
import org.apache.syncope.common.lib.types.OIDCClientAuthenticationMethod;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionAlg;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAOIDCRPClientApp.TABLE)
public class JPAOIDCRPClientApp extends AbstractClientApp implements OIDCRPClientApp {

    private static final long serialVersionUID = 7422422526695279794L;

    public static final String TABLE = "OIDCRPClientApp";

    protected static final TypeReference<Set<String>> STRING_TYPEREF = new TypeReference<Set<String>>() {
    };

    protected static final TypeReference<Set<OIDCGrantType>> GRANT_TYPE_TYPEREF =
            new TypeReference<Set<OIDCGrantType>>() {
    };

    protected static final TypeReference<Set<OIDCResponseType>> RESPONSE_TYPE_TYPEREF =
            new TypeReference<Set<OIDCResponseType>>() {
    };

    protected static final TypeReference<Set<String>> SCOPE_TYPEREF =
            new TypeReference<Set<String>>() {
    };

    @Column(unique = true, nullable = false)
    private String clientId;

    private String clientSecret;

    private String idTokenIssuer;

    private boolean signIdToken = true;

    @Enumerated(EnumType.STRING)
    private OIDCTokenSigningAlg idTokenSigningAlg = OIDCTokenSigningAlg.none;

    private boolean encryptIdToken;

    @Enumerated(EnumType.STRING)
    private OIDCTokenEncryptionAlg idTokenEncryptionAlg = OIDCTokenEncryptionAlg.none;

    @Enumerated(EnumType.STRING)
    private OIDCTokenEncryptionEncoding idTokenEncryptionEncoding;

    @Enumerated(EnumType.STRING)
    private OIDCTokenSigningAlg userInfoSigningAlg;

    @Enumerated(EnumType.STRING)
    private OIDCTokenEncryptionAlg userInfoEncryptedResponseAlg;

    @Enumerated(EnumType.STRING)
    private OIDCTokenEncryptionEncoding userInfoEncryptedResponseEncoding;

    private boolean jwtAccessToken;

    private boolean bypassApprovalPrompt = true;

    private boolean generateRefreshToken = true;

    @Enumerated(EnumType.STRING)
    private OIDCSubjectType subjectType = OIDCSubjectType.PUBLIC;

    @Enumerated(EnumType.STRING)
    private OIDCApplicationType applicationType = OIDCApplicationType.WEB;

    @Lob
    private String redirectUris;

    @Transient
    private Set<String> redirectUrisSet = new HashSet<>();

    @Lob
    private String supportedGrantTypes;

    @Transient
    private Set<OIDCGrantType> supportedGrantTypesSet = new HashSet<>();

    @Lob
    private String supportedResponseTypes;

    @Transient
    private Set<OIDCResponseType> supportedResponseTypesSet = new HashSet<>();

    @Lob
    private String scopes;

    @Transient
    private Set<String> scopesSet = new HashSet<>();

    @Lob
    private String jwks;

    private String jwksUri;

    @Enumerated(EnumType.STRING)
    private OIDCClientAuthenticationMethod tokenEndpointAuthenticationMethod;

    private String logoutUri;

    @Override
    public Set<String> getRedirectUris() {
        return redirectUrisSet;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Override
    public String getIdTokenIssuer() {
        return idTokenIssuer;
    }

    @Override
    public void setIdTokenIssuer(final String idTokenIssuer) {
        this.idTokenIssuer = idTokenIssuer;
    }

    @Override
    public boolean isSignIdToken() {
        return signIdToken;
    }

    @Override
    public void setSignIdToken(final boolean signIdToken) {
        this.signIdToken = signIdToken;
    }

    @Override
    public OIDCTokenSigningAlg getIdTokenSigningAlg() {
        return idTokenSigningAlg;
    }

    @Override
    public void setIdTokenSigningAlg(final OIDCTokenSigningAlg idTokenSigningAlg) {
        this.idTokenSigningAlg = idTokenSigningAlg;
    }

    @Override
    public boolean isEncryptIdToken() {
        return encryptIdToken;
    }

    @Override
    public void setEncryptIdToken(final boolean encryptIdToken) {
        this.encryptIdToken = encryptIdToken;
    }

    @Override
    public OIDCTokenEncryptionAlg getIdTokenEncryptionAlg() {
        return idTokenEncryptionAlg;
    }

    @Override
    public void setIdTokenEncryptionAlg(final OIDCTokenEncryptionAlg idTokenEncryptionAlg) {
        this.idTokenEncryptionAlg = idTokenEncryptionAlg;
    }

    @Override
    public OIDCTokenEncryptionEncoding getIdTokenEncryptionEncoding() {
        return idTokenEncryptionEncoding;
    }

    @Override
    public void setIdTokenEncryptionEncoding(final OIDCTokenEncryptionEncoding idTokenEncryptionEncoding) {
        this.idTokenEncryptionEncoding = idTokenEncryptionEncoding;
    }

    @Override
    public OIDCTokenSigningAlg getUserInfoSigningAlg() {
        return userInfoSigningAlg;
    }

    @Override
    public void setUserInfoSigningAlg(final OIDCTokenSigningAlg userInfoSigningAlg) {
        this.userInfoSigningAlg = userInfoSigningAlg;
    }

    @Override
    public OIDCTokenEncryptionAlg getUserInfoEncryptedResponseAlg() {
        return userInfoEncryptedResponseAlg;
    }

    @Override
    public void setUserInfoEncryptedResponseAlg(final OIDCTokenEncryptionAlg userInfoEncryptedResponseAlg) {
        this.userInfoEncryptedResponseAlg = userInfoEncryptedResponseAlg;
    }

    @Override
    public OIDCTokenEncryptionEncoding getUserInfoEncryptedResponseEncoding() {
        return userInfoEncryptedResponseEncoding;
    }

    @Override
    public void setUserInfoEncryptedResponseEncoding(final OIDCTokenEncryptionEncoding encoding) {
        this.userInfoEncryptedResponseEncoding = encoding;
    }

    @Override
    public boolean isJwtAccessToken() {
        return jwtAccessToken;
    }

    @Override
    public void setJwtAccessToken(final boolean jwtAccessToken) {
        this.jwtAccessToken = jwtAccessToken;
    }

    @Override
    public boolean isBypassApprovalPrompt() {
        return bypassApprovalPrompt;
    }

    @Override
    public void setBypassApprovalPrompt(final boolean bypassApprovalPrompt) {
        this.bypassApprovalPrompt = bypassApprovalPrompt;
    }

    @Override
    public boolean isGenerateRefreshToken() {
        return generateRefreshToken;
    }

    @Override
    public void setGenerateRefreshToken(final boolean generateRefreshToken) {
        this.generateRefreshToken = generateRefreshToken;
    }

    @Override
    public OIDCSubjectType getSubjectType() {
        return subjectType;
    }

    @Override
    public void setSubjectType(final OIDCSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    @Override
    public OIDCApplicationType getApplicationType() {
        return applicationType;
    }

    @Override
    public void setApplicationType(final OIDCApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    @Override
    public Set<OIDCGrantType> getSupportedGrantTypes() {
        return supportedGrantTypesSet;
    }

    @Override
    public Set<OIDCResponseType> getSupportedResponseTypes() {
        return supportedResponseTypesSet;
    }

    @Override
    public Set<String> getScopes() {
        return scopesSet;
    }

    @Override
    public String getJwks() {
        return jwks;
    }

    @Override
    public void setJwks(final String jwks) {
        this.jwks = jwks;
    }

    @Override
    public String getJwksUri() {
        return jwksUri;
    }

    @Override
    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    public OIDCClientAuthenticationMethod getTokenEndpointAuthenticationMethod() {
        return tokenEndpointAuthenticationMethod;
    }

    @Override
    public void setTokenEndpointAuthenticationMethod(
            final OIDCClientAuthenticationMethod tokenEndpointAuthenticationMethod) {

        this.tokenEndpointAuthenticationMethod = tokenEndpointAuthenticationMethod;
    }

    @Override
    public String getLogoutUri() {
        return logoutUri;
    }

    @Override
    public void setLogoutUri(final String logoutUri) {
        this.logoutUri = logoutUri;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getRedirectUris().clear();
            getSupportedGrantTypes().clear();
            getSupportedResponseTypes().clear();
        }
        if (redirectUris != null) {
            getRedirectUris().addAll(POJOHelper.deserialize(redirectUris, STRING_TYPEREF));
        }
        if (supportedGrantTypes != null) {
            getSupportedGrantTypes().addAll(POJOHelper.deserialize(supportedGrantTypes, GRANT_TYPE_TYPEREF));
        }
        if (supportedResponseTypes != null) {
            getSupportedResponseTypes().addAll(POJOHelper.deserialize(supportedResponseTypes, RESPONSE_TYPE_TYPEREF));
        }
        if (scopes != null) {
            getScopes().addAll(POJOHelper.deserialize(scopes, SCOPE_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        redirectUris = POJOHelper.serialize(getRedirectUris());
        supportedGrantTypes = POJOHelper.serialize(getSupportedGrantTypes());
        supportedResponseTypes = POJOHelper.serialize(getSupportedResponseTypes());
        scopes = POJOHelper.serialize(getScopes());
    }
}
