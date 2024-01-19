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
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.jws.MSEntraAccessTokenJWSVerifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * JWT authorisation for access tokens issued by Microsoft Entra (formerly Azure)
 * for Microsoft Entra-only applications (v1.0 tokens)
 * cf. https://learn.microsoft.com/en-us/entra/identity-platform/access-tokens
 */
public class MSEntraJWTSSOProvider implements JWTSSOProvider {

    protected final UserDAO userDAO;

    protected final AuthDataAccessor authDataAccessor;

    protected final String tenantId;

    protected final String appId;

    protected final String authUsername;

    protected final Duration clockSkew;

    protected final MSEntraAccessTokenJWSVerifier verifier;

    public MSEntraJWTSSOProvider(
            final UserDAO userDAO,
            final AuthDataAccessor authDataAccessor,
            final String tenantId,
            final String appId,
            final String authUsername,
            final Duration clockSkew,
            final MSEntraAccessTokenJWSVerifier verifier) {

        this.userDAO = userDAO;
        this.authDataAccessor = authDataAccessor;
        this.tenantId = tenantId;
        this.appId = appId;
        this.authUsername = authUsername;
        this.clockSkew = clockSkew;
        this.verifier = verifier;
    }

    @Override
    public String getIssuer() {
        return String.format("https://sts.windows.net/%s/", tenantId);
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return verifier.supportedJWSAlgorithms();
    }

    @Override
    public JCAContext getJCAContext() {
        return verifier.getJCAContext();
    }

    /*
     * When parsing the token, you must [...] ensure the token meets these requirements:
     *
     * - The token was sent in the HTTP Authorization header with "Bearer" scheme.
     * - The token is valid JSON that conforms to the JWT standard.
     * - The token contains an "issuer" claim with one of the highlighted values for non-governmental cases.
     * - The token contains an "audience" claim with a value equal to the Microsoft App ID.
     * - The token is within its validity period. Industry-standard clock-skew is 5 minutes.
     * - The token has a valid cryptographic signature with a key listed in the OpenID keys document that was retrieved
     * from the `jwks_uri` property in the OpenID metadata document via GET request.
     *
     * cf. https://learn.microsoft.com/en-us/entra/identity-platform/security-tokens#validate-security-tokens
     */
    @Override
    public boolean verify(final JWSHeader header, final byte[] signingInput, final Base64URL signature)
            throws JOSEException {

        return verifier.verify(header, signingInput, signature);
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<User, Set<SyncopeGrantedAuthority>> resolve(final JWTClaimsSet jwtClaims) {
        User authUser = userDAO.findByUsername(authUsername).orElse(null);
        Set<SyncopeGrantedAuthority> authorities = Set.of();

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = jwtClaims.getIssueTime().toInstant();
        Instant notBefore = jwtClaims.getNotBeforeTime().toInstant();
        Instant expired = jwtClaims.getExpirationTime().toInstant();

        if (authUser != null
                && jwtClaims.getAudience().contains(appId)
                && now.isAfter(issued.minus(clockSkew))
                && now.isAfter(notBefore.minus(clockSkew))
                && now.isBefore(expired.plus(clockSkew))) {

            authorities = authDataAccessor.getAuthorities(authUser.getUsername(), null);
        }

        return Pair.of(authUser, authorities);
    }
}
