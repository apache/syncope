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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

class AuthDataAccessorTest {

    private static final String LOW_PRIVILEGE_JTI = "low-privilege-users-valid-token-id";

    private static final String LOW_PRIVILEGE_USER = "low-priv-user";

    private SecurityProperties securityProperties;

    private AccessTokenDAO accessTokenDAO;

    private AuthDataAccessor authDataAccessor;

    @BeforeEach
    void setUp() throws Exception {
        securityProperties = new SecurityProperties();
        securityProperties.setJwtIssuer("ApacheSyncope");
        securityProperties.setJwsAlgorithm("HS512");
        securityProperties.setJwsKey(SecureRandomUtils.generateRandomLetters(64));
        securityProperties.setAdminUser("admin");

        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        AccessToken lowPrivilegeToken = mock(AccessToken.class);
        when(lowPrivilegeToken.getOwner()).thenReturn(LOW_PRIVILEGE_USER);
        accessTokenDAO = mock(AccessTokenDAO.class);
        doReturn(Optional.of(lowPrivilegeToken)).when(accessTokenDAO).findById(LOW_PRIVILEGE_JTI);

        Constructor<?> constructor = Arrays.stream(AuthDataAccessor.class.getConstructors()).
                findFirst().orElseThrow();
        Object[] arguments = Arrays.stream(constructor.getParameterTypes()).map(type -> {
            if (type == SecurityProperties.class) {
                return securityProperties;
            }
            if (type == AccessTokenDAO.class) {
                return accessTokenDAO;
            }
            if (type == List.class) {
                return List.of();
            }
            return mock(type);
        }).toArray();

        authDataAccessor = (AuthDataAccessor) constructor.newInstance(arguments);
    }

    @Test
    void lowPrivilegeTokenIdCanBeReboundToAdminWithDefaultSigningKey() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().
                issuer(securityProperties.getJwtIssuer()).
                subject(securityProperties.getAdminUser()).
                jwtID(LOW_PRIVILEGE_JTI).
                issueTime(new Date()).
                notBeforeTime(new Date()).
                expirationTime(Date.from(OffsetDateTime.now().plusMinutes(10).toInstant())).
                build();
        SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        forged.sign(new AccessTokenJWSSigner(JWSAlgorithm.HS512, securityProperties.getJwsKey()));

        assertTrue(forged.verify(new AccessTokenJWSVerifier(JWSAlgorithm.HS512, securityProperties.getJwsKey())));

        JWTAuthentication authentication = new JWTAuthentication(
                forged.getJWTClaimsSet(),
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));

        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> authDataAccessor.authenticate(authentication));
        assertEquals("Access Token owner does not match JWT subject", ex.getMessage());
    }
}
