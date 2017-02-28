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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.security.core.Authentication;

/**
 * Represents the token for an authentication request or for an authenticated principal as JSON Web Token,
 * once the request has been processed by the
 * {@link org.springframework.security.authentication.AuthenticationManager#authenticate(Authentication)} method.
 */
public class JWTAuthentication implements Authentication {

    private static final long serialVersionUID = -2013733709281305394L;

    private final JwtClaims claims;

    private final SyncopeAuthenticationDetails details;

    private final Set<SyncopeGrantedAuthority> authorities = new HashSet<>();

    private boolean authenticated = false;

    public JWTAuthentication(final JwtClaims claims, final SyncopeAuthenticationDetails details) {
        this.claims = claims;
        this.details = details;
    }

    public JwtClaims getClaims() {
        return claims;
    }

    @Override
    public Collection<SyncopeGrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return StringUtils.EMPTY;
    }

    @Override
    public SyncopeAuthenticationDetails getDetails() {
        return details;
    }

    @Override
    public Object getPrincipal() {
        return claims.getSubject();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(final boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated;
    }

    @Override
    public String getName() {
        return claims.getSubject();
    }
}
