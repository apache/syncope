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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Processes the JSON Web Token provided as {@link HttpHeaders#AUTHORIZATION} HTTP header, putting the result into the
 * {@link SecurityContextHolder}.
 */
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private AuthenticationEntryPoint authenticationEntryPoint;

    private AuthenticationManager authenticationManager;

    private SyncopeAuthenticationDetailsSource authenticationDetailsSource;

    @Autowired
    private AuthDataAccessor dataAccessor;

    @Autowired
    private DefaultCredentialChecker credentialChecker;

    public void setAuthenticationEntryPoint(final AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    public void setAuthenticationManager(final AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public void setAuthenticationDetailsSource(final SyncopeAuthenticationDetailsSource authenticationDetailsSource) {
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.authenticationEntryPoint, "An AuthenticationEntryPoint is required");
        Assert.notNull(this.authenticationManager, "An AuthenticationManager is required");
        Assert.notNull(this.authenticationDetailsSource, "AuthenticationDetailsSource required");
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String[] parts = auth == null ? null : auth.split(" ");
        if (parts == null || parts.length != 2 || !"Bearer".equals(parts[0])) {
            chain.doFilter(request, response);
            return;
        }

        String stringToken = parts[1];
        LOG.debug("JWT received: {}", stringToken);

        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(stringToken);
        try {
            credentialChecker.checkIsDefaultJWSKeyInUse();

            JWTSSOProvider jwtSSOProvider = dataAccessor.getJWTSSOProvider(consumer.getJwtClaims().getIssuer());
            if (!consumer.verifySignatureWith(jwtSSOProvider)) {
                throw new BadCredentialsException("Invalid signature found in JWT");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new JWTAuthentication(consumer.getJwtClaims(), authenticationDetailsSource.buildDetails(request)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            this.authenticationEntryPoint.commence(request, response, e);
        }
    }
}
