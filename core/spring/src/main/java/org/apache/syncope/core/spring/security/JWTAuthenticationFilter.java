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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Processes the JSON Web Token provided as {@link HttpHeaders#AUTHORIZATION} HTTP header, putting the result into the
 * {@link SecurityContextHolder}.
 */
public class JWTAuthenticationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final SyncopeAuthenticationDetailsSource authenticationDetailsSource;

    private final AuthDataAccessor dataAccessor;

    private final DefaultCredentialChecker credentialChecker;

    public JWTAuthenticationFilter(
            final AuthenticationManager authenticationManager,
            final AuthenticationEntryPoint authenticationEntryPoint,
            final SyncopeAuthenticationDetailsSource authenticationDetailsSource,
            final AuthDataAccessor dataAccessor,
            final DefaultCredentialChecker credentialChecker) {

        super(authenticationManager);
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationDetailsSource = authenticationDetailsSource;
        this.dataAccessor = dataAccessor;
        this.credentialChecker = credentialChecker;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String[] parts = Optional.ofNullable(auth).map(s -> s.split(" ")).orElse(null);
        if (parts == null || parts.length != 2 || !"Bearer".equals(parts[0])) {
            chain.doFilter(request, response);
            return;
        }

        String stringToken = parts[1];
        LOG.debug("JWT received: {}", stringToken);

        try {
            credentialChecker.checkIsDefaultJWSKeyInUse();

            // 0. parse JWT
            SignedJWT jwt = SignedJWT.parse(stringToken);

            // 1. check signature
            JWTSSOProvider jwtSSOProvider = dataAccessor.getJWTSSOProvider(jwt.getJWTClaimsSet().getIssuer());
            if (!jwt.verify(jwtSSOProvider)) {
                throw new BadCredentialsException("Invalid signature found in JWT");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            long referenceTime = System.currentTimeMillis();

            // 2. check expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && expirationTime.getTime() < referenceTime) {
                dataAccessor.removeExpired(claims.getJWTID());
                throw new CredentialsExpiredException("JWT is expired");
            }

            // 3. check not before
            Date notBefore = claims.getNotBeforeTime();
            if (notBefore != null && notBefore.getTime() > referenceTime) {
                throw new CredentialsExpiredException("JWT not valid yet");
            }

            // 4. generate and set the authentication object
            JWTAuthentication jwtAuthentication =
                    new JWTAuthentication(claims, authenticationDetailsSource.buildDetails(request));
            jwtAuthentication.setAuthenticated(true);
            AuthContextUtils.runAsAdmin(jwtAuthentication.getDetails().getDomain(), () -> {
                Pair<String, Set<SyncopeGrantedAuthority>> authenticated = dataAccessor.authenticate(jwtAuthentication);
                jwtAuthentication.setUsername(authenticated.getLeft());
                jwtAuthentication.getAuthorities().addAll(authenticated.getRight());
            });
            SecurityContextHolder.getContext().setAuthentication(jwtAuthentication);

            chain.doFilter(request, response);
        } catch (ParseException | JOSEException e) {
            SecurityContextHolder.clearContext();
            this.authenticationEntryPoint.commence(
                    request, response, new BadCredentialsException("Invalid JWT: " + stringToken, e));
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            this.authenticationEntryPoint.commence(request, response, e);
        }
    }
}
