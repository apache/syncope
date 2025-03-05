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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.JWTSSOProvider;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation for internal JWT validation.
 */
public class CustomJWTSSOProvider implements JWTSSOProvider {

    public static final String ISSUER = "custom-issuer";

    public static final String CUSTOM_KEY =
            "XW3eTdntLa9Zsz2t4Vm6TNUya8xJEezFS7NVD3ZIZKOMdmSPMfi40rIvyBzXdbqD7TTsp6grcVW3AvRhZnFzZNaLdp6kJ2HXU9X9t2arVK"
            + "42bIAp7XOw6aZg8v4OOXReZ9YkuAKtGwKC1JvPMKCz0c28AhJWd3YX5MpG6prXExQpFFVuweA6xTPxf06nYEFSOmKJ9ddJAcIx4Z8qyY"
            + "mDJyNscMU8eVVM7aCR9zrCAHnjRZI2i6OnStAEVuqfGL25tK9AUKPVvyWljHNZ6ugXkstF873QaYJTBst1U2Zl9XsZnyeKrFEwwVHipp"
            + "vfHwo2xu6VKySyJpZtaqVrjXFqpgFGRwEm890tCm8JhEG6GgJPqcnFHrYC180LqBZSjnNQGvA7eCSFVrABWcWnXDJCIHWbn0Wv153Vf4"
            + "ZH75XEEYY53KsOS2T2GAmoqV3Izz7RL8O5dntgNLevl5gZb6MbYFURnQt0vALeObxMmv459FsXinzpAVihriOZWAudpN6Q";

    private final JWSVerifier delegate;

    private final AnySearchDAO anySearchDAO;

    private final AuthDataAccessor authDataAccessor;

    public CustomJWTSSOProvider(
            final AnySearchDAO anySearchDAO,
            final AuthDataAccessor authDataAccessor)
            throws JOSEException {

        this.delegate = new MACVerifier(CUSTOM_KEY);
        this.anySearchDAO = anySearchDAO;
        this.authDataAccessor = authDataAccessor;
    }

    @Override
    public String getIssuer() {
        return ISSUER;
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
        AttrCond userIdCond = new AttrCond();
        userIdCond.setSchema("userId");
        userIdCond.setType(AttrCond.Type.EQ);
        userIdCond.setExpression(jwtClaims.getSubject());

        List<User> matching = anySearchDAO.search(SearchCond.of(userIdCond), AnyTypeKind.USER);
        if (matching.size() == 1) {
            User user = matching.getFirst();
            Set<SyncopeGrantedAuthority> authorities = authDataAccessor.getAuthorities(user.getUsername(), null);

            return Pair.of(user, authorities);
        }

        return null;
    }
}
