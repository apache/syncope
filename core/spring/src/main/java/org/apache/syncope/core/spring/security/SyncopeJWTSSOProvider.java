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

import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsVerificationSignature;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.jws.AccessTokenJwsSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation for internal JWT validation.
 */
public class SyncopeJWTSSOProvider implements JWTSSOProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeJWTSSOProvider.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Resource(name = "jwtIssuer")
    private String jwtIssuer;

    @Autowired
    private AccessTokenJwsSignatureVerifier delegate;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Override
    public String getIssuer() {
        return jwtIssuer;
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public boolean verify(final JwsHeaders headers, final String unsignedText, final byte[] signature) {
        return delegate.verify(headers, unsignedText, signature);
    }

    @Override
    public JwsVerificationSignature createJwsVerificationSignature(final JwsHeaders headers) {
        return delegate.createJwsVerificationSignature(headers);
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<User, Set<SyncopeGrantedAuthority>> resolve(final JwtClaims jwtClaims) {
        User user = userDAO.findByUsername(jwtClaims.getSubject());
        Set<SyncopeGrantedAuthority> authorities = Set.of();
        if (user != null) {
            AccessToken accessToken = accessTokenDAO.find(jwtClaims.getTokenId());
            if (accessToken != null && accessToken.getAuthorities() != null) {
                try {
                    authorities = POJOHelper.deserialize(
                            ENCRYPTOR.decode(new String(accessToken.getAuthorities()), CipherAlgorithm.AES),
                            new TypeReference<Set<SyncopeGrantedAuthority>>() {
                    });
                } catch (Throwable t) {
                    LOG.error("Could not read stored authorities", t);
                }
            }
        }

        return Pair.of(user, authorities);
    }
}
