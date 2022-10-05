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
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
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

    @Column(unique = true, nullable = false)
    private String clientId;

    private String clientSecret;

    private boolean signIdToken;

    private boolean jwtAccessToken;

    private boolean bypassApprovalPrompt = true;

    @Enumerated(EnumType.STRING)
    private OIDCSubjectType subjectType;

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
    public boolean isSignIdToken() {
        return signIdToken;
    }

    @Override
    public void setSignIdToken(final boolean signIdToken) {
        this.signIdToken = signIdToken;
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
    public OIDCSubjectType getSubjectType() {
        return subjectType;
    }

    @Override
    public void setSubjectType(final OIDCSubjectType subjectType) {
        this.subjectType = subjectType;
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
    }
}
