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
package org.apache.syncope.core.misc.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContextUtil {

    public static String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? SyncopeConstants.UNAUTHENTICATED : authentication.getName();
    }

    public static Set<String> getOwnedEntitlementNames() {
        final Set<String> result = new HashSet<>();

        final SecurityContext ctx = SecurityContextHolder.getContext();

        if (ctx != null && ctx.getAuthentication() != null && ctx.getAuthentication().getAuthorities() != null) {
            for (GrantedAuthority authority : ctx.getAuthentication().getAuthorities()) {
                result.add(authority.getAuthority());
            }
        }

        return result;
    }

    /**
     * Extend the current authentication context to include the given role.
     *
     * @param roleKey role key
     * @param roleEntitlement role entitlement
     */
    public static void extendAuthContext(final Long roleKey, final String roleEntitlement) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = new ArrayList<>(auth.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(roleEntitlement));
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                auth.getPrincipal(), auth.getCredentials(), authorities);
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private AuthContextUtil() {
    }
}
