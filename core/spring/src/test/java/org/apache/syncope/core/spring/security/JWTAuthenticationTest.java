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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.junit.jupiter.api.Test;

class JWTAuthenticationTest {

    private static JWTClaimsSet claims(final String subject) {
        return new JWTClaimsSet.Builder().
                issuer("issuer").
                subject(subject).
                jwtID("jti").
                build();
    }

    private static SyncopeAuthenticationDetails details() {
        return new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "delegator");
    }

    @Test
    void initialState() {
        JWTClaimsSet claims = claims("bob");
        SyncopeAuthenticationDetails details = details();
        JWTAuthentication authentication = new JWTAuthentication(claims, details);

        assertSame(claims, authentication.getClaims());
        assertSame(details, authentication.getDetails());
        assertEquals("", authentication.getCredentials());
        assertEquals("bob", authentication.getPrincipal());
        assertEquals("bob", authentication.getName());
        assertFalse(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().isEmpty());
    }

    @Test
    void setUsername() {
        JWTAuthentication authentication = new JWTAuthentication(claims("bob"), details());

        authentication.setUsername("alice");

        assertEquals("alice", authentication.getPrincipal());
        assertEquals("alice", authentication.getName());
    }

    @Test
    void authoritiesAreMutable() {
        JWTAuthentication authentication = new JWTAuthentication(claims("bob"), details());
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ);

        authentication.getAuthorities().add(authority);

        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(authority));
    }

    @Test
    void setAuthenticated() {
        JWTAuthentication authentication = new JWTAuthentication(claims("bob"), details());

        authentication.setAuthenticated(true);

        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void equalsAndHashCode() {
        JWTAuthentication first = new JWTAuthentication(claims("bob"), details());
        JWTAuthentication second = new JWTAuthentication(claims("bob"), details());
        JWTAuthentication differentSubject = new JWTAuthentication(claims("alice"), details());

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, differentSubject);
        assertNotEquals(first, null);
        assertNotEquals(first, "other");

        first.getAuthorities().add(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ));
        assertNotEquals(first, second);
    }

    @Test
    void toStringContainsState() {
        JWTAuthentication authentication = new JWTAuthentication(claims("bob"), details());

        assertTrue(authentication.toString().contains("Principal: bob"));
        assertTrue(authentication.toString().contains("Authenticated: false"));
        assertTrue(authentication.toString().contains("Not granted any authorities"));

        authentication.getAuthorities().add(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ));
        assertTrue(authentication.toString().contains("Granted Authorities:"));
    }
}
