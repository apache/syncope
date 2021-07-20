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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRPClientApp;

@Entity
@Table(name = JPAOIDCRPClientApp.TABLE)
public class JPAOIDCRPClientApp extends AbstractClientApp implements OIDCRPClientApp {

    private static final long serialVersionUID = 7422422526695279794L;

    public static final String TABLE = "OIDCRPClientApp";

    @Column(unique = true, nullable = false)
    private String clientId;

    private String clientSecret;

    private boolean signIdToken;

    private boolean jwtAccessToken;

    @Enumerated(EnumType.STRING)
    private OIDCSubjectType subjectType;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "redirectUri")
    @CollectionTable(name = "OIDCRPClientApp_RedirectUris",
            joinColumns =
            @JoinColumn(name = "client_id", referencedColumnName = "id"))
    private Set<String> redirectUris = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "supportedGrantType")
    @CollectionTable(name = "OIDCRPClientApp_SupportedGrantTypes",
            joinColumns =
            @JoinColumn(name = "client_id", referencedColumnName = "id"))
    private Set<OIDCGrantType> supportedGrantTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "supportedResponseType")
    @CollectionTable(name = "OIDCRPClientApp_SupportedResponseTypes",
            joinColumns =
            @JoinColumn(name = "client_id", referencedColumnName = "id"))
    private Set<OIDCResponseType> supportedResponseTypes = new HashSet<>();

    private String logoutUri;

    @Override
    public Set<String> getRedirectUris() {
        return redirectUris;
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
    public OIDCSubjectType getSubjectType() {
        return subjectType;
    }

    @Override
    public void setSubjectType(final OIDCSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    @Override
    public Set<OIDCGrantType> getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    @Override
    public Set<OIDCResponseType> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    @Override
    public String getLogoutUri() {
        return logoutUri;
    }

    @Override
    public void setLogoutUri(final String logoutUri) {
        this.logoutUri = logoutUri;
    }
}
