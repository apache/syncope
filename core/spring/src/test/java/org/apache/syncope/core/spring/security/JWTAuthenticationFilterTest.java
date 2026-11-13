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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

class JWTAuthenticationFilterTest {

    private static final String TOKEN = "header.payload.signature";

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

    private final AuthenticationEntryPoint authenticationEntryPoint = mock(AuthenticationEntryPoint.class);

    private final AuthDataAccessor dataAccessor = mock(AuthDataAccessor.class);

    private final DefaultCredentialChecker credentialChecker = mock(DefaultCredentialChecker.class);

    private final JWTSSOProvider jwtSSOProvider = mock(JWTSSOProvider.class);

    private StaticApplicationContext context;

    private JWTAuthenticationFilter filter;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private FilterChain chain;

    private ConfigurableApplicationContext previousContext;

    private DefaultListableBeanFactory previousBeanFactory;

    @BeforeEach
    void setUp() {
        previousContext = ApplicationContextProvider.getApplicationContext();
        previousBeanFactory = ApplicationContextProvider.getBeanFactory();

        SecurityContextHolder.clearContext();
        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");

        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("securityProperties", securityProperties);
        context.refresh();

        ApplicationContextProvider.setApplicationContext(context);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) context.getBeanFactory());

        filter = new JWTAuthenticationFilter(
                authenticationManager,
                authenticationEntryPoint,
                new SyncopeAuthenticationDetailsSource(),
                dataAccessor,
                credentialChecker);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        when(dataAccessor.getJWTSSOProvider(any())).thenReturn(jwtSSOProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ApplicationContextProvider.setApplicationContext(previousContext);
        ApplicationContextProvider.setBeanFactory(previousBeanFactory);
        context.close();
    }

    private void bearerToken() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().
                issuer("issuer").
                subject("bob").
                jwtID("jti");
    }

    private SignedJWT signedJWT(final JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = mock(SignedJWT.class);
        when(jwt.getJWTClaimsSet()).thenReturn(claims);
        when(jwt.verify(jwtSSOProvider)).thenReturn(true);
        return jwt;
    }

    @Test
    void noAuthorizationHeaderPassesThrough() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void nonBearerAuthorizationPassesThrough() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        bearerToken();

        try (MockedStatic<SignedJWT> mocked = mockStatic(SignedJWT.class)) {
            mocked.when(() -> SignedJWT.parse(TOKEN)).thenThrow(new ParseException("bad token", 0));

            filter.doFilterInternal(request, response, chain);
        }

        verify(chain, never()).doFilter(any(), any());
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(BadCredentialsException.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidSignatureIsRejected() throws Exception {
        bearerToken();
        JWTClaimsSet claims = claims().build();
        SignedJWT jwt = mock(SignedJWT.class);
        when(jwt.getJWTClaimsSet()).thenReturn(claims);
        when(jwt.verify(jwtSSOProvider)).thenReturn(false);

        try (MockedStatic<SignedJWT> mocked = mockStatic(SignedJWT.class)) {
            mocked.when(() -> SignedJWT.parse(TOKEN)).thenReturn(jwt);

            filter.doFilterInternal(request, response, chain);
        }

        verify(chain, never()).doFilter(any(), any());
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(BadCredentialsException.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredTokenIsRemovedAndRejected() throws Exception {
        bearerToken();
        JWTClaimsSet claims = claims().
                expirationTime(new Date(System.currentTimeMillis() - 60_000)).
                build();
        SignedJWT jwt = signedJWT(claims);

        try (MockedStatic<SignedJWT> mocked = mockStatic(SignedJWT.class)) {
            mocked.when(() -> SignedJWT.parse(TOKEN)).thenReturn(jwt);

            filter.doFilterInternal(request, response, chain);
        }

        verify(dataAccessor).removeExpired("jti");
        verify(chain, never()).doFilter(any(), any());
        verify(authenticationEntryPoint).commence(
                eq(request), eq(response), any(CredentialsExpiredException.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void notYetValidTokenIsRejected() throws Exception {
        bearerToken();
        JWTClaimsSet claims = claims().
                notBeforeTime(new Date(System.currentTimeMillis() + 60_000)).
                build();
        SignedJWT jwt = signedJWT(claims);

        try (MockedStatic<SignedJWT> mocked = mockStatic(SignedJWT.class)) {
            mocked.when(() -> SignedJWT.parse(TOKEN)).thenReturn(jwt);

            filter.doFilterInternal(request, response, chain);
        }

        verify(dataAccessor, never()).removeExpired(any());
        verify(chain, never()).doFilter(any(), any());
        verify(authenticationEntryPoint).commence(
                eq(request), eq(response), any(CredentialsExpiredException.class));
    }

    @Test
    void validTokenSetsAuthentication() throws Exception {
        bearerToken();
        JWTClaimsSet claims = claims().build();
        SignedJWT jwt = signedJWT(claims);

        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ);
        when(dataAccessor.authenticate(any())).thenReturn(
                new AuthDataAccessor.JWTAuthResult("alice", Set.of(authority)));

        try (MockedStatic<SignedJWT> mocked = mockStatic(SignedJWT.class)) {
            mocked.when(() -> SignedJWT.parse(TOKEN)).thenReturn(jwt);

            filter.doFilterInternal(request, response, chain);
        }

        verify(chain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());

        JWTAuthentication authentication =
                assertInstanceOf(JWTAuthentication.class, SecurityContextHolder.getContext().getAuthentication());
        assertTrue(authentication.isAuthenticated());
        assertEquals("alice", authentication.getName());
        assertEquals(Set.of(authority), authentication.getAuthorities());
        assertEquals("jti", authentication.getClaims().getJWTID());
    }

    @Test
    void authenticationFailureCommencesEntryPoint() throws Exception {
        bearerToken();
        JWTClaimsSet claims = claims().build();
        SignedJWT jwt = signedJWT(claims);
        when(dataAccessor.authenticate(any())).thenThrow(new BadCredentialsException("no"));

        try (MockedStatic<SignedJWT> mocked = mockStatic(SignedJWT.class)) {
            mocked.when(() -> SignedJWT.parse(TOKEN)).thenReturn(jwt);

            filter.doFilterInternal(request, response, chain);
        }

        verify(chain, never()).doFilter(any(), any());
        verify(authenticationEntryPoint).commence(
                eq(request), eq(response), any(AuthenticationException.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
