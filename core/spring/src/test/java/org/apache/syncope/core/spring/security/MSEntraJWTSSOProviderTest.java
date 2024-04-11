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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import javax.cache.CacheManager;
import javax.cache.integration.CacheLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.jws.MSEntraAccessTokenJWSVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MSEntraJWTSSOProviderTest {

    private static final String TENANT_ID = "test-tenant-id";

    private static final String APP_ID = "test-app-id";

    private static final String AUTH_USERNAME = "auth-username";

    @Mock
    private CacheManager cacheManager;

    @Mock
    private CacheLoader<String, JWSVerifier> cacheLoader;

    @Mock
    private User user;

    @Mock
    private UserDAO userDAO;

    @Mock
    private AuthDataAccessor authDataAccessor;

    private MSEntraAccessTokenJWSVerifier verifier() {
        return new MSEntraAccessTokenJWSVerifier(cacheManager, cacheLoader, javax.cache.expiry.Duration.ONE_DAY);
    }

    @Test
    void getIssuer() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        assertEquals(provider.getIssuer(), "https://sts.windows.net/" + TENANT_ID + "/");
    }

    @Test
    void resolveSuccess() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));
        when(authDataAccessor.getAuthorities(AUTH_USERNAME, null)).
                thenReturn(Set.of(mock(SyncopeGrantedAuthority.class)));
        when(user.getUsername()).thenReturn(AUTH_USERNAME);

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(65, ChronoUnit.SECONDS);
        Instant notBefore = now.minus(5, ChronoUnit.SECONDS);
        Instant expiration = now.plus(1, ChronoUnit.HOURS);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        Pair<User, Set<SyncopeGrantedAuthority>> resolved = provider.resolve(payload);
        assertEquals(AUTH_USERNAME, resolved.getKey().getUsername());
        assertEquals(1, resolved.getValue().size());
    }

    @Test
    void resolveMissingClaims() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .build();

        assertThrows(Exception.class, () -> provider.resolve(payload));
    }

    @Test
    void resolveAuthUserNull() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.empty());

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(1, ChronoUnit.MINUTES);
        Instant notBefore = now.minus(1, ChronoUnit.SECONDS);
        Instant expiration = now.plus(59, ChronoUnit.MINUTES);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        Pair<User, Set<SyncopeGrantedAuthority>> resolved = provider.resolve(payload);
        assertNull(resolved.getKey());
        assertTrue(resolved.getValue().isEmpty());
    }

    @Test
    void resolveWrongAudience() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(1, ChronoUnit.MINUTES);
        Instant notBefore = now.minus(1, ChronoUnit.SECONDS);
        Instant expiration = now.plus(59, ChronoUnit.MINUTES);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience("wrong-audience-claim")
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertTrue(provider.resolve(payload).getValue().isEmpty());
    }

    @Test
    void resolveIssuedFail() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.plus(6, ChronoUnit.MINUTES);
        Instant notBefore = now.minus(5, ChronoUnit.SECONDS);
        Instant expiration = now.plus(1, ChronoUnit.HOURS);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertTrue(provider.resolve(payload).getValue().isEmpty());
    }

    @Test
    void resolveIssuedInClockSkew() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));
        when(authDataAccessor.getAuthorities(AUTH_USERNAME, null)).
                thenReturn(Set.of(mock(SyncopeGrantedAuthority.class)));
        when(user.getUsername()).thenReturn(AUTH_USERNAME);

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.plus(4, ChronoUnit.MINUTES);
        Instant notBefore = now.minus(5, ChronoUnit.SECONDS);
        Instant expiration = now.plus(1, ChronoUnit.HOURS);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertEquals(1, provider.resolve(payload).getValue().size());
    }

    @Test
    void resolveNotBeforeFail() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(1, ChronoUnit.MINUTES);
        Instant notBefore = now.plus(6, ChronoUnit.MINUTES);
        Instant expiration = now.plus(1, ChronoUnit.HOURS);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertTrue(provider.resolve(payload).getValue().isEmpty());
    }

    @Test
    void resolveNotBeforeInClockSkew() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));
        when(authDataAccessor.getAuthorities(AUTH_USERNAME, null)).
                thenReturn(Set.of(mock(SyncopeGrantedAuthority.class)));
        when(user.getUsername()).thenReturn(AUTH_USERNAME);

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(1, ChronoUnit.MINUTES);
        Instant notBefore = now.plus(4, ChronoUnit.MINUTES);
        Instant expiration = now.plus(1, ChronoUnit.HOURS);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertEquals(1, provider.resolve(payload).getValue().size());
    }

    @Test
    void resolveExpirationFail() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(1, ChronoUnit.HOURS);
        Instant notBefore = now.minus(1, ChronoUnit.HOURS);
        Instant expiration = now.minus(6, ChronoUnit.MINUTES);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertTrue(provider.resolve(payload).getValue().isEmpty());
    }

    @Test
    void resolveExpirationInClockSkew() {
        MSEntraJWTSSOProvider provider = new MSEntraJWTSSOProvider(
                userDAO, authDataAccessor, TENANT_ID, APP_ID, AUTH_USERNAME, Duration.ofMinutes(5), verifier());

        when(userDAO.findByUsername(anyString())).thenAnswer(ic -> Optional.of(user));
        when(authDataAccessor.getAuthorities(AUTH_USERNAME, null)).
                thenReturn(Set.of(mock(SyncopeGrantedAuthority.class)));
        when(user.getUsername()).thenReturn(AUTH_USERNAME);

        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        Instant issued = now.minus(1, ChronoUnit.HOURS);
        Instant notBefore = now.minus(1, ChronoUnit.HOURS);
        Instant expiration = now.minus(4, ChronoUnit.MINUTES);

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .issueTime(Date.from(issued))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .build();

        assertEquals(1, provider.resolve(payload).getValue().size());
    }
}
