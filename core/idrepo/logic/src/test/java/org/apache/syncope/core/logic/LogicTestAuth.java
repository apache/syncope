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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper to set / clear the Spring Security context in unit tests, without bootstrapping a full application
 * context.
 */
public final class LogicTestAuth {

    private static final String PASSWORD = "FAKE_PASSWORD";

    public static void authenticate(final String username) {
        authenticate(username, SyncopeConstants.MASTER_DOMAIN, Map.of());
    }

    public static void authenticate(final String username, final String domain) {
        authenticate(username, domain, Map.of());
    }

    public static void authenticate(
            final String username,
            final String domain,
            final Map<String, Set<String>> entitlements) {

        List<GrantedAuthority> authorities = entitlements.entrySet().stream().
                map(entry -> new SyncopeGrantedAuthority(entry.getKey(), entry.getValue().iterator().next())).
                map(GrantedAuthority.class::cast).
                toList();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(username, PASSWORD, authorities),
                PASSWORD,
                authorities);
        authentication.setDetails(new SyncopeAuthenticationDetails(domain, null));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }

    private LogicTestAuth() {
    }
}
