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
package org.apache.syncope.fit.core.reference;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsVerificationSignature;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.JWTAccessToken;
import org.apache.syncope.core.spring.security.JWTSSOProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation for internal JWT validation.
 */
public class CustomJWTSSOProvider implements JWTSSOProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CustomJWTSSOProvider.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    public static final String ISSUER = "custom-issuer";

    public static final String CUSTOM_KEY = "12345678910987654321";

    private final JwsSignatureVerifier delegate;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private AuthDataAccessor authDataAccessor;

    public CustomJWTSSOProvider() {
        delegate = new HmacJwsSignatureVerifier(CUSTOM_KEY.getBytes(), SignatureAlgorithm.HS512);
    }

    @Override
    public String getIssuer() {
        return ISSUER;
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
    public Pair<User, AccessToken> resolve(final JwtClaims jwtClaims) {
        AttributeCond userIdCond = new AttributeCond();
        userIdCond.setSchema("userId");
        userIdCond.setType(AttributeCond.Type.EQ);
        userIdCond.setExpression(jwtClaims.getSubject());

        List<User> matching = searchDAO.search(SearchCond.getLeafCond(userIdCond), AnyTypeKind.USER);
        if (matching.size() == 1) {
            User user = matching.get(0);

            AccessToken accessToken = null;
            try {
                accessToken = new JWTAccessToken(jwtClaims);
                accessToken.setAuthorities(ENCRYPTOR.encode(
                        POJOHelper.serialize(authDataAccessor.getAuthorities(user.getUsername())), CipherAlgorithm.AES).
                        getBytes());
            } catch (Exception e) {
                LOG.error("Could not fetch or store authorities", e);
            }

            return Pair.of(user, accessToken);
        }

        return null;
    }

}
