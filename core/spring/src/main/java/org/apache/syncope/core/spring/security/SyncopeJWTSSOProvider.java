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

import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation for internal JWT validation.
 */
public class SyncopeJWTSSOProvider implements JWTSSOProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeJWTSSOProvider.class);

    protected final SecurityProperties securityProperties;

    protected final EncryptorManager encryptorManager;

    protected final AccessTokenJWSVerifier delegate;

    protected final UserDAO userDAO;

    protected final AccessTokenDAO accessTokenDAO;

    public SyncopeJWTSSOProvider(
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final AccessTokenJWSVerifier delegate,
            final UserDAO userDAO,
            final AccessTokenDAO accessTokenDAO) {

        this.securityProperties = securityProperties;
        this.encryptorManager = encryptorManager;
        this.delegate = delegate;
        this.userDAO = userDAO;
        this.accessTokenDAO = accessTokenDAO;
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
    public Pair<User, Set<SyncopeGrantedAuthority>> resolve(final JWTClaimsSet jwtClaims) {
        User user = userDAO.findByUsername(jwtClaims.getSubject()).orElse(null);
        Set<SyncopeGrantedAuthority> authorities = Set.of();
        if (user != null) {
            AccessToken accessToken = accessTokenDAO.findById(jwtClaims.getJWTID()).orElse(null);
            if (accessToken != null && accessToken.getAuthorities() != null) {
                try {
                    authorities = POJOHelper.deserialize(
                            encryptorManager.getInstance().decode(
                                    new String(accessToken.getAuthorities()), CipherAlgorithm.AES),
                            new TypeReference<>() {
                    });
                } catch (Throwable t) {
                    LOG.error("Could not read stored authorities", t);
                }
            }
        }

        return Pair.of(user, authorities);
    }
}
