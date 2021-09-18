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

import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Date;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Attempts to authenticate the passed {@link JWTAuthentication} object, returning a fully populated
 * {@link Authentication} object (including granted authorities) if successful.
 */
public class JWTAuthenticationProvider implements AuthenticationProvider {

    protected final AuthDataAccessor dataAccessor;

    public JWTAuthenticationProvider(final AuthDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        JWTAuthentication jwtAuthentication = (JWTAuthentication) authentication;

        JWTClaimsSet claims = jwtAuthentication.getClaims();
        long referenceTime = System.currentTimeMillis();

        Date expirationTime = claims.getExpirationTime();
        if (expirationTime != null && expirationTime.getTime() < referenceTime) {
            dataAccessor.removeExpired(claims.getJWTID());
            throw new CredentialsExpiredException("JWT is expired");
        }

        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && notBefore.getTime() > referenceTime) {
            throw new CredentialsExpiredException("JWT not valid yet");
        }

        jwtAuthentication.setAuthenticated(true);
        return jwtAuthentication;
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return JWTAuthentication.class.isAssignableFrom(authentication);
    }
}
