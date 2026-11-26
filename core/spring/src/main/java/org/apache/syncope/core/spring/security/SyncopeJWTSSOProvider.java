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
package org.apache.syncope.core.spring.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation for internal JWT validation.
 */
public class SyncopeJWTSSOProvider implements JWTSSOProvider {

    protected final SecurityProperties securityProperties;

    protected final AccessTokenJWSVerifier delegate;

    protected final UserDAO userDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final AccessTokenManager accessTokenManager;

    protected final AuthDataAccessor authDataAccessor;

    public SyncopeJWTSSOProvider(
            final SecurityProperties securityProperties,
            final AccessTokenJWSVerifier delegate,
            final UserDAO userDAO,
            final AccessTokenDAO accessTokenDAO,
            final AccessTokenManager accessTokenManager,
            final AuthDataAccessor authDataAccessor) {

        this.securityProperties = securityProperties;
        this.delegate = delegate;
        this.userDAO = userDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.accessTokenManager = accessTokenManager;
        this.authDataAccessor = authDataAccessor;
    }

    @Override
    public String getIssuer() {
        return securityProperties.getJwtIssuer();
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return delegate.supportedJWSAlgorithms();
    }

    @Override
    public JCAContext getJCAContext() {
        return delegate.getJCAContext();
    }

    @Override
    public boolean verify(
            final JWSHeader header,
            final byte[] signingInput,
            final Base64URL signature) throws JOSEException {

        return delegate.verify(header, signingInput, signature);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ResolvedClaims> resolve(final JWTClaimsSet jwtClaims) {
        AccessToken accessToken = accessTokenDAO.findById(jwtClaims.getJWTID()).
                orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Could not find an Access Token for JWT " + jwtClaims.getJWTID()));

        if (!jwtClaims.getSubject().equals(accessToken.getOwner())) {
            throw new AuthenticationCredentialsNotFoundException("Access Token owner does not match JWT subject");
        }

        return userDAO.findByUsername(jwtClaims.getSubject()).map(user -> {
            // Resolve the authorities from the current user state, with a short-lived cache to avoid hitting the
            // database on every single request.
            Set<SyncopeGrantedAuthority> authorities = accessTokenManager.getAuthorities(
                    AuthContextUtils.getDomain(),
                    user.getUsername(),
                    () -> authDataAccessor.getAuthorities(user.getUsername(), null));

            return new ResolvedClaims(user, authorities);
        });
    }
}
