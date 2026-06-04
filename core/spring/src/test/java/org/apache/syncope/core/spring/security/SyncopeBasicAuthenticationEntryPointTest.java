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

import org.apache.syncope.common.rest.api.RESTHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class SyncopeBasicAuthenticationEntryPointTest {

    private SyncopeBasicAuthenticationEntryPoint entryPoint;

    @BeforeEach
    public void setUp() throws Exception {
        entryPoint = new SyncopeBasicAuthenticationEntryPoint();
        entryPoint.setRealmName("Apache Syncope authentication");
        entryPoint.afterPropertiesSet();
    }

    @Test
    void rateLimitAuthenticationExceptionReturnsTooManyRequests() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new RateLimitAuthenticationException(30));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        assertEquals("30", response.getHeader(HttpHeaders.RETRY_AFTER));
        assertEquals(SyncopeBasicAuthenticationEntryPoint.AUTHENTICATION_FAILED,
                response.getHeader(RESTHeaders.ERROR_INFO));
    }

    @Test
    public void badCredentialsExposeGenericErrorInfo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("rossini: invalid password provided"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(SyncopeBasicAuthenticationEntryPoint.AUTHENTICATION_FAILED,
                response.getHeader(RESTHeaders.ERROR_INFO));
    }

    @Test
    public void missingUserExposeGenericErrorInfo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new UsernameNotFoundException("not-a-user"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(SyncopeBasicAuthenticationEntryPoint.AUTHENTICATION_FAILED,
                response.getHeader(RESTHeaders.ERROR_INFO));
    }

    @Test
    public void disabledUserExposeGenericErrorInfo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new DisabledException("User rossini is suspended"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(SyncopeBasicAuthenticationEntryPoint.AUTHENTICATION_FAILED,
                response.getHeader(RESTHeaders.ERROR_INFO));
    }
}
