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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

class SinglePurposeEntitlementFilterTest {

    private final SinglePurposeEntitlementFilter filter = new SinglePurposeEntitlementFilter();

    private FilterChain chain;

    @BeforeEach
    void setUp() {
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(final String... entitlements) {
        List<SyncopeGrantedAuthority> authorities = java.util.Arrays.stream(entitlements).
                map(SyncopeGrantedAuthority::new).
                toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bob", null, authorities));
    }

    private SecurityContextHolderAwareRequestWrapper wrapper(final String method, final String pathInfo) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getPathInfo()).thenReturn(pathInfo);
        return new SecurityContextHolderAwareRequestWrapper(request, "ROLE_");
    }

    @Test
    void notWrappedRequestPassesThrough() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void mustChangePasswordOnOtherEndpointIsDenied() throws Exception {
        authenticate(IdRepoEntitlement.MUST_CHANGE_PASSWORD);
        SecurityContextHolderAwareRequestWrapper request = wrapper("GET", "/users");

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> filter.doFilter(request, mock(ServletResponse.class), chain));

        assertEquals("Please change your password first", ex.getMessage());
        SingleEntitlementException cause = assertInstanceOf(SingleEntitlementException.class, ex.getCause());
        assertEquals(IdRepoEntitlement.MUST_CHANGE_PASSWORD, cause.getEntitlement());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void mustChangePasswordOnExpectedEndpointPassesThrough() throws Exception {
        authenticate(IdRepoEntitlement.MUST_CHANGE_PASSWORD);
        SecurityContextHolderAwareRequestWrapper request = wrapper("POST", "/users/self/mustChangePassword");
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void mustChangePasswordWithWrongMethodIsDenied() throws Exception {
        authenticate(IdRepoEntitlement.MUST_CHANGE_PASSWORD);
        SecurityContextHolderAwareRequestWrapper request = wrapper("GET", "/users/self/mustChangePassword");

        assertThrows(
                AccessDeniedException.class,
                () -> filter.doFilter(request, mock(ServletResponse.class), chain));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void mfaEnrollOnExpectedEndpointPassesThrough() throws Exception {
        authenticate(IdRepoEntitlement.MFA_ENROLL);
        SecurityContextHolderAwareRequestWrapper request = wrapper("PUT", "/mfa");
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void mfaEnrollOnOtherEndpointIsDenied() throws Exception {
        authenticate(IdRepoEntitlement.MFA_ENROLL);
        SecurityContextHolderAwareRequestWrapper request = wrapper("PUT", "/users");

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> filter.doFilter(request, mock(ServletResponse.class), chain));

        assertEquals("Please enroll your MFA first", ex.getMessage());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void noSinglePurposeEntitlementPassesThrough() throws Exception {
        authenticate(IdRepoEntitlement.USER_READ);
        SecurityContextHolderAwareRequestWrapper request = wrapper("GET", "/users");
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void initAndDestroyAreNoOps() {
        filter.init(null);
        filter.destroy();
    }
}
