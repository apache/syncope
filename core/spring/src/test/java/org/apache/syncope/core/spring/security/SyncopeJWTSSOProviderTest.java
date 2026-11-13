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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

@ExtendWith(MockitoExtension.class)
class SyncopeJWTSSOProviderTest {

    private static final String JWT_ID = "jwt-id";

    private static final String USERNAME = "bob";

    private static final String ISSUER = "configured-issuer";

    @Mock
    private EncryptorManager encryptorManager;

    @Mock
    private Encryptor encryptor;

    @Mock
    private AccessTokenJWSVerifier delegate;

    @Mock
    private UserDAO userDAO;

    @Mock
    private AccessTokenDAO accessTokenDAO;

    @Mock
    private AccessToken accessToken;

    @Mock
    private User user;

    @Mock
    private JWSHeader header;

    @Mock
    private JCAContext jcaContext;

    private SecurityProperties securityProperties;

    private SyncopeJWTSSOProvider provider;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setJwtIssuer(ISSUER);

        provider = new SyncopeJWTSSOProvider(
                securityProperties, encryptorManager, delegate, userDAO, accessTokenDAO);
    }

    private JWTClaimsSet claims(final String subject, final String jwtID) {
        return new JWTClaimsSet.Builder().subject(subject).jwtID(jwtID).build();
    }

    private void mockAccessToken(final String owner) {
        when(accessToken.getOwner()).thenReturn(owner);
        doReturn(Optional.of(accessToken)).when(accessTokenDAO).findById(JWT_ID);
    }

    @Test
    void getIssuer() {
        assertEquals(ISSUER, provider.getIssuer());
    }

    @Test
    void supportedJWSAlgorithms() {
        Set<JWSAlgorithm> algorithms = Set.of(JWSAlgorithm.HS512);
        when(delegate.supportedJWSAlgorithms()).thenReturn(algorithms);

        assertSame(algorithms, provider.supportedJWSAlgorithms());
    }

    @Test
    void getJCAContext() {
        when(delegate.getJCAContext()).thenReturn(jcaContext);

        assertSame(jcaContext, provider.getJCAContext());
    }

    @Test
    void verifyDelegatesToJWSVerifier() throws JOSEException {
        byte[] signingInput = new byte[] { 1, 2, 3 };
        Base64URL signature = new Base64URL("signature");
        when(delegate.verify(header, signingInput, signature)).thenReturn(true);

        assertTrue(provider.verify(header, signingInput, signature));
        verify(delegate).verify(header, signingInput, signature);
    }

    @Test
    void verifyPropagatesJOSEException() throws JOSEException {
        byte[] signingInput = new byte[] { 1, 2, 3 };
        Base64URL signature = new Base64URL("signature");
        when(delegate.verify(header, signingInput, signature)).thenThrow(new JOSEException("boom"));

        JOSEException ex = assertThrows(
                JOSEException.class,
                () -> provider.verify(header, signingInput, signature));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void resolveThrowsWhenAccessTokenIsMissing() {
        doReturn(Optional.empty()).when(accessTokenDAO).findById(JWT_ID);

        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> provider.resolve(claims(USERNAME, JWT_ID)));
        assertEquals("Could not find an Access Token for JWT " + JWT_ID, ex.getMessage());
    }

    @Test
    void resolveThrowsWhenAccessTokenOwnerDoesNotMatchSubject() {
        mockAccessToken("alice");

        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> provider.resolve(claims(USERNAME, JWT_ID)));
        assertEquals("Access Token owner does not match JWT subject", ex.getMessage());
    }

    @Test
    void resolveReturnsEmptyWhenUserCannotBeFound() {
        mockAccessToken(USERNAME);
        doReturn(Optional.empty()).when(userDAO).findByUsername(USERNAME);

        assertTrue(provider.resolve(claims(USERNAME, JWT_ID)).isEmpty());
        verifyNoInteractions(encryptorManager);
    }

    @Test
    void resolveReturnsUserWithoutAuthoritiesWhenAccessTokenHasNone() {
        mockAccessToken(USERNAME);
        doReturn(Optional.of(user)).when(userDAO).findByUsername(USERNAME);

        JWTSSOProvider.ResolvedClaims resolved = provider.resolve(claims(USERNAME, JWT_ID)).orElseThrow();

        assertSame(user, resolved.user());
        assertTrue(resolved.authorities().isEmpty());
        verifyNoInteractions(encryptorManager);
    }

    @Test
    void resolveDecodesStoredAuthorities() throws Exception {
        Set<SyncopeGrantedAuthority> stored = Set.of(
                new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/users"));
        mockAccessToken(USERNAME);
        when(accessToken.getAuthorities()).thenReturn("encoded-authorities");
        when(encryptorManager.getInstance()).thenReturn(encryptor);
        when(encryptor.decode("encoded-authorities", CipherAlgorithm.AES)).
                thenReturn(POJOHelper.serialize(stored));
        doReturn(Optional.of(user)).when(userDAO).findByUsername(USERNAME);

        JWTSSOProvider.ResolvedClaims resolved = provider.resolve(claims(USERNAME, JWT_ID)).orElseThrow();

        assertEquals(1, resolved.authorities().size());
        SyncopeGrantedAuthority authority = resolved.authorities().iterator().next();
        assertEquals(IdRepoEntitlement.USER_READ, authority.getAuthority());
        assertEquals(Set.of("/users"), authority.getRealms());
        verify(encryptor).decode("encoded-authorities", CipherAlgorithm.AES);
    }

    @Test
    void resolveReturnsUserWithoutAuthoritiesWhenDecodingFails() throws Exception {
        mockAccessToken(USERNAME);
        when(accessToken.getAuthorities()).thenReturn("encoded-authorities");
        when(encryptorManager.getInstance()).thenReturn(encryptor);
        when(encryptor.decode("encoded-authorities", CipherAlgorithm.AES)).
                thenThrow(new IllegalStateException("boom"));
        doReturn(Optional.of(user)).when(userDAO).findByUsername(USERNAME);

        JWTSSOProvider.ResolvedClaims resolved = provider.resolve(claims(USERNAME, JWT_ID)).orElseThrow();

        assertSame(user, resolved.user());
        assertTrue(resolved.authorities().isEmpty());
    }

    @Test
    void resolveReturnsUserWithoutAuthoritiesWhenDeserializationFails() throws Exception {
        mockAccessToken(USERNAME);
        when(accessToken.getAuthorities()).thenReturn("encoded-authorities");
        when(encryptorManager.getInstance()).thenReturn(encryptor);
        when(encryptor.decode("encoded-authorities", CipherAlgorithm.AES)).thenReturn("not-json");
        doReturn(Optional.of(user)).when(userDAO).findByUsername(USERNAME);

        JWTSSOProvider.ResolvedClaims resolved = provider.resolve(claims(USERNAME, JWT_ID)).orElseThrow();

        assertSame(user, resolved.user());
        assertTrue(resolved.authorities().isEmpty());
    }

    @Test
    void resolveReturnsUserWithoutAuthoritiesWhenEncryptorManagerFails() {
        mockAccessToken(USERNAME);
        when(accessToken.getAuthorities()).thenReturn("encoded-authorities");
        when(encryptorManager.getInstance()).thenThrow(new IllegalStateException("boom"));
        doReturn(Optional.of(user)).when(userDAO).findByUsername(USERNAME);

        JWTSSOProvider.ResolvedClaims resolved = provider.resolve(claims(USERNAME, JWT_ID)).orElseThrow();

        assertSame(user, resolved.user());
        assertTrue(resolved.authorities().isEmpty());
    }
}
