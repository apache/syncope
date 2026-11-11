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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.oidc.OIDCRequest;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.logic.oidc.OIDCClientCache;
import org.apache.syncope.core.logic.oidc.OIDCUserManager;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.SessionKeyCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.credentials.OidcCredentials;

class OIDCC4UILogicTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef";

    private static Method logoutMethod() {
        try {
            return OIDCC4UILogic.class.getMethod("createLogoutRequest", String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Method METHOD = logoutMethod();

    private OIDCClientCache loginCache;

    private OIDCClientCache logoutCache;

    private AuthDataAccessor authDataAccessor;

    private AccessTokenDataBinder accessTokenDataBinder;

    private OIDCC4UIProviderDAO opDAO;

    private AccessTokenDAO accessTokenDAO;

    private OIDCUserManager userManager;

    private Encryptor encryptor;

    private OIDCC4UILogic logic;

    @BeforeEach
    void setUp() {
        loginCache = mock(OIDCClientCache.class);
        logoutCache = mock(OIDCClientCache.class);
        authDataAccessor = mock(AuthDataAccessor.class);
        accessTokenDataBinder = mock(AccessTokenDataBinder.class);
        opDAO = mock(OIDCC4UIProviderDAO.class);
        accessTokenDAO = mock(AccessTokenDAO.class);
        userManager = mock(OIDCUserManager.class);

        EncryptorManager encryptorManager = mock(EncryptorManager.class);
        encryptor = mock(Encryptor.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);

        logic = new OIDCC4UILogic(
                loginCache,
                logoutCache,
                authDataAccessor,
                accessTokenDataBinder,
                opDAO,
                accessTokenDAO,
                userManager,
                encryptorManager,
                mock(SessionStore.class));
    }

    private static OIDCC4UIProvider provider() {
        OIDCC4UIProvider op = mock(OIDCC4UIProvider.class);
        when(op.getName()).thenReturn("op");
        when(op.getIssuer()).thenReturn("https://op.example.com");
        when(op.getEndSessionEndpoint()).thenReturn("https://op.example.com/logout");
        when(op.getItems()).thenReturn(List.of());
        when(op.getConnObjectKeyItem()).thenReturn(Optional.of(new Item()));
        return op;
    }

    private static String signedJwt(final JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(KEY));
        return jwt.serialize();
    }

    private static OidcClient oidcClient() {
        OidcClient client = mock(OidcClient.class);
        when(client.getCallbackUrl()).thenReturn("https://client.example.com/callback");
        return client;
    }

    @Test
    void createLoginRequestMissingProvider() {
        doReturn(Optional.empty()).when(opDAO).findByName("op");

        assertThrows(NotFoundException.class, () -> logic.createLoginRequest("https://client/callback", "op", false));
    }

    @Test
    void createLoginRequestUsesExistingClient() {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));
        when(client.getRedirectionAction(any(CallContext.class))).
                thenReturn(Optional.of(new FoundAction("https://op.example.com/authorize")));

        OIDCRequest request = logic.createLoginRequest("https://client.example.com/callback", "op", false);

        assertEquals("noop", request.id());
        assertEquals("https://op.example.com/authorize", request.location());
        verify(loginCache, never()).add(any(), anyString());
    }

    @Test
    void createLoginRequestAddsClientWhenMissing() {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.empty());
        when(loginCache.add(op, "https://client.example.com/callback")).thenReturn(client);
        when(client.getRedirectionAction(any(CallContext.class))).
                thenReturn(Optional.of(new FoundAction("https://op.example.com/authorize")));

        assertEquals("https://op.example.com/authorize",
                logic.createLoginRequest("https://client.example.com/callback", "op", false).location());
        verify(loginCache).add(op, "https://client.example.com/callback");
    }

    @Test
    void createLoginRequestFailsWithoutRedirectionAction() {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));
        when(client.getRedirectionAction(any(CallContext.class))).thenReturn(Optional.empty());

        SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                () -> logic.createLoginRequest("https://client.example.com/callback", "op", false));
        assertEquals(ClientExceptionType.Unknown, sce.getType());
    }

    @Test
    void loginReturnsTokensAndAttributes() throws Exception {
        OIDCC4UIProvider op = provider();
        Item email = new Item();
        email.setExtAttrName("email");
        when(op.getItems()).thenReturn(List.of(email));
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));

        JWTClaimsSet claims = new JWTClaimsSet.Builder().
                subject("sub").
                claim("email", "john@example.com").
                build();
        String idToken = new PlainJWT(claims).serialize();

        Authenticator authenticator = mock(Authenticator.class);
        when(client.getAuthenticator()).thenReturn(authenticator);
        doAnswer(invocation -> {
            OidcCredentials credentials = invocation.getArgument(1);
            credentials.setIdToken(idToken);
            return null;
        }).when(authenticator).validate(any(CallContext.class), any(Credentials.class));

        when(userManager.findMatchingUser(eq("sub"), any(Item.class))).thenReturn(List.of("john"));
        when(op.isUpdateMatching()).thenReturn(false);

        when(encryptor.encode(anyString(), eq(CipherAlgorithm.AES))).thenReturn("AUTH");
        when(authDataAccessor.getAuthorities("john", null)).thenReturn(Set.of());

        OffsetDateTime expiration = OffsetDateTime.now().plusMinutes(5);
        when(accessTokenDataBinder.create(
                eq(Optional.empty()), eq("john"), anyMap(), eq("AUTH"), eq(true))).
                thenReturn(new AccessTokenDataBinder.AccessTokenInfo("jwt", expiration));

        OIDCLoginResponse response = logic.login("requestId", "https://client/callback", "code", "op");

        assertEquals("john", response.getUsername());
        assertEquals("jwt", response.getAccessToken());
        assertEquals(expiration, response.getAccessTokenExpiryTime());
        assertTrue(response.isLogoutSupported());
        assertFalse(response.isSelfReg());
        assertEquals("john@example.com", response.getAttr("email").orElseThrow().getValues().getFirst());
        assertTrue(response.getAttrs().stream().noneMatch(attr -> "sub".equals(attr.getSchema())));
    }

    @Test
    void loginSelfRegistersUnmatchingUser() throws Exception {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");
        when(op.isSelfRegUnmatching()).thenReturn(true);

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));

        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("sub").build();
        String idToken = new PlainJWT(claims).serialize();

        Authenticator authenticator = mock(Authenticator.class);
        when(client.getAuthenticator()).thenReturn(authenticator);
        doAnswer(invocation -> {
            OidcCredentials credentials = invocation.getArgument(1);
            credentials.setIdToken(idToken);
            return null;
        }).when(authenticator).validate(any(CallContext.class), any(Credentials.class));

        when(userManager.findMatchingUser(eq("sub"), any(Item.class))).thenReturn(List.of());

        OIDCLoginResponse response = logic.login("requestId", "https://client/callback", "code", "op");

        assertTrue(response.isSelfReg());
        assertEquals("sub", response.getUsername());
        assertEquals(null, response.getAccessToken());
        verify(accessTokenDataBinder, never()).create(any(), anyString(), anyMap(), any(), eq(true));
    }

    @Test
    void loginFailsWhenValidationFails() {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));

        Authenticator authenticator = mock(Authenticator.class);
        when(client.getAuthenticator()).thenReturn(authenticator);
        doAnswer(invocation -> {
            throw new IllegalStateException("broken");
        }).when(authenticator).validate(any(CallContext.class), any(Credentials.class));

        SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                () -> logic.login("requestId", "https://client/callback", "code", "op"));
        assertEquals(ClientExceptionType.Unknown, sce.getType());
        assertTrue(sce.getElements().contains("broken"));
    }

    @Test
    void loginFailsWithoutClaims() {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));
        when(client.getAuthenticator()).thenReturn(mock(Authenticator.class));

        SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                () -> logic.login("requestId", "https://client/callback", "code", "op"));
        assertEquals(ClientExceptionType.Unknown, sce.getType());
        assertTrue(sce.getElements().contains("Unable to extract OIDC claims"));
    }

    @Test
    void loginFailsWhenSeveralUsersMatch() throws Exception {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(loginCache.get("op")).thenReturn(Optional.of(client));

        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("sub").build();
        Authenticator authenticator = mock(Authenticator.class);
        when(client.getAuthenticator()).thenReturn(authenticator);
        doAnswer(invocation -> {
            OidcCredentials credentials = invocation.getArgument(1);
            credentials.setIdToken(new PlainJWT(claims).serialize());
            return null;
        }).when(authenticator).validate(any(CallContext.class), any(Credentials.class));

        when(userManager.findMatchingUser(eq("sub"), any(Item.class))).thenReturn(List.of("john", "jane"));

        assertThrows(IllegalArgumentException.class,
                () -> logic.login("requestId", "https://client/callback", "code", "op"));
    }

    @Test
    void createLogoutRequestParsesAccessToken() throws Exception {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(logoutCache.get("op")).thenReturn(Optional.of(client));
        when(client.getLogoutAction(any(CallContext.class), any(), eq("https://client.example.com/callback"))).
                thenReturn(Optional.of(new FoundAction("https://op.example.com/logout")));

        String accessToken = signedJwt(new JWTClaimsSet.Builder().
                claim(OIDCC4UILogic.JWT_CLAIM_OP_NAME, "op").
                claim(OIDCC4UILogic.JWT_CLAIM_ID_TOKEN, "idToken").
                build());

        OIDCRequest request = logic.createLogoutRequest(accessToken, "https://client.example.com/callback");

        assertEquals("noop", request.id());
        assertEquals("https://op.example.com/logout", request.location());
    }

    @Test
    void createLogoutRequestRejectsInvalidToken() {
        SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                () -> logic.createLogoutRequest("not-a-jwt", "https://client.example.com/callback"));
        assertEquals(ClientExceptionType.ExecutionError, sce.getType());
    }

    @Test
    void createLogoutRequestMissingProvider() throws Exception {
        doReturn(Optional.empty()).when(opDAO).findByName("op");

        String accessToken = signedJwt(new JWTClaimsSet.Builder().
                claim(OIDCC4UILogic.JWT_CLAIM_OP_NAME, "op").
                build());

        assertThrows(NotFoundException.class,
                () -> logic.createLogoutRequest(accessToken, "https://client.example.com/callback"));
    }

    @Test
    void createLogoutRequestFailsWithoutLogoutAction() throws Exception {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(logoutCache.get("op")).thenReturn(Optional.of(client));
        when(client.getLogoutAction(any(CallContext.class), any(), anyString())).thenReturn(Optional.empty());

        String accessToken = signedJwt(new JWTClaimsSet.Builder().
                claim(OIDCC4UILogic.JWT_CLAIM_OP_NAME, "op").
                build());

        assertThrows(SyncopeClientException.class,
                () -> logic.createLogoutRequest(accessToken, "https://client.example.com/callback"));
    }

    @Test
    void backChannelLogoutDeletesAccessToken() throws Exception {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(logoutCache.get("op")).thenReturn(Optional.of(client));

        SessionKeyCredentials credentials = new SessionKeyCredentials();
        credentials.setSessionKey("sessionKey");
        when(client.getCredentials(any(CallContext.class))).thenReturn(Optional.of(credentials));

        String logoutToken = signedJwt(new JWTClaimsSet.Builder().audience("op").build());

        logic.backChannelLogout(logoutToken, "https://client.example.com/callback");

        verify(accessTokenDAO).deleteById("sessionKey");
    }

    @Test
    void backChannelLogoutRejectsInvalidToken() {
        SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                () -> logic.backChannelLogout("not-a-jwt", "https://client.example.com/callback"));
        assertEquals(ClientExceptionType.ExecutionError, sce.getType());
    }

    @Test
    void backChannelLogoutMissingProvider() throws Exception {
        doReturn(Optional.empty()).when(opDAO).findByName("op");

        String logoutToken = signedJwt(new JWTClaimsSet.Builder().audience("op").build());

        assertThrows(NotFoundException.class,
                () -> logic.backChannelLogout(logoutToken, "https://client.example.com/callback"));
        verify(accessTokenDAO, never()).deleteById(anyString());
    }

    @Test
    void backChannelLogoutFailsWhenTokenCannotBeValidated() throws Exception {
        OIDCC4UIProvider op = provider();
        doReturn(Optional.of(op)).when(opDAO).findByName("op");

        OidcClient client = oidcClient();
        when(logoutCache.get("op")).thenReturn(Optional.of(client));
        when(client.getCredentials(any(CallContext.class))).thenReturn(Optional.empty());

        String logoutToken = signedJwt(new JWTClaimsSet.Builder().audience("op").build());

        SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                () -> logic.backChannelLogout(logoutToken, "https://client.example.com/callback"));
        assertEquals(ClientExceptionType.Unknown, sce.getType());
        assertTrue(sce.getElements().contains("Could not validate the logout token"));
    }

    @Test
    void resolveReferenceIsNotSupported() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(METHOD, "arg"));
    }
}
