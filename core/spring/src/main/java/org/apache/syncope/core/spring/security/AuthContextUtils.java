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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public final class AuthContextUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AuthContextUtils.class);

    public interface Executable<T> {

        T exec();
    }

    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? SyncopeConstants.UNAUTHENTICATED : authentication.getName();
    }

    public static void updateUsername(final String newUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                new User(newUsername, "FAKE_PASSWORD", auth.getAuthorities()),
                auth.getCredentials(), auth.getAuthorities());
        newAuth.setDetails(auth.getDetails());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    public static Map<String, Set<String>> getAuthorizations() {
        Map<String, Set<String>> result = null;

        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null && ctx.getAuthentication() != null && ctx.getAuthentication().getAuthorities() != null) {
            result = new HashMap<>();
            for (GrantedAuthority authority : ctx.getAuthentication().getAuthorities()) {
                if (authority instanceof SyncopeGrantedAuthority) {
                    result.put(
                            SyncopeGrantedAuthority.class.cast(authority).getAuthority(),
                            SyncopeGrantedAuthority.class.cast(authority).getRealms());
                }
            }
        }

        return MapUtils.emptyIfNull(result);
    }

    public static String getDomain() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String domainKey = auth != null && auth.getDetails() instanceof SyncopeAuthenticationDetails
                ? SyncopeAuthenticationDetails.class.cast(auth.getDetails()).getDomain()
                : null;
        if (StringUtils.isBlank(domainKey)) {
            domainKey = SyncopeConstants.MASTER_DOMAIN;
        }

        return domainKey;
    }

    private static void setFakeAuth(final String domain) {
        List<GrantedAuthority> authorities = CollectionUtils.collect(EntitlementsHolder.getInstance().getValues(),
                new Transformer<String, GrantedAuthority>() {

            @Override
            public GrantedAuthority transform(final String entitlement) {
                return new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM);
            }
        }, new ArrayList<GrantedAuthority>());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new User(ApplicationContextProvider.getBeanFactory().getBean("adminUser", String.class),
                        "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(domain));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static <T> T execWithAuthContext(final String domainKey, final Executable<T> executable) {
        SecurityContext ctx = SecurityContextHolder.getContext();
        setFakeAuth(domainKey);
        try {
            return executable.exec();
        } catch (Throwable t) {
            LOG.debug("Error during execution with domain {} context", domainKey, t);
            throw t;
        } finally {
            SecurityContextHolder.clearContext();
            SecurityContextHolder.setContext(ctx);
        }
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private AuthContextUtils() {
    }
}
