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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public final class AuthContextUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AuthContextUtils.class);

    private static final String FAKE_PASSWORD = "FAKE_PASSWORD";

    private static final String UNAUTHENTICATED = "unauthenticated";

    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? UNAUTHENTICATED : authentication.getName();
    }

    public static void updateUsername(final String newUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                new User(newUsername, FAKE_PASSWORD, auth.getAuthorities()),
                auth.getCredentials(), auth.getAuthorities());
        newAuth.setDetails(auth.getDetails());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    public static Optional<String> getDelegatedBy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null && auth.getDetails() instanceof SyncopeAuthenticationDetails
                ? Optional.ofNullable(SyncopeAuthenticationDetails.class.cast(auth.getDetails()).getDelegatedBy())
                : Optional.empty();
    }

    public static String getWho() {
        return getUsername() + getDelegatedBy().map(d -> {
            String delegatedBy = callAsAdmin(getDomain(),
                    () -> ApplicationContextProvider.getApplicationContext().getBean(UserDAO.class).findUsername(d)).
                    orElse(d);
            return " [delegated by " + delegatedBy + "]";
        }).orElse(StringUtils.EMPTY);
    }

    public static Set<SyncopeGrantedAuthority> getAuthorities() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()).
                map(authentication -> authentication.getAuthorities().stream().
                filter(SyncopeGrantedAuthority.class::isInstance).
                map(SyncopeGrantedAuthority.class::cast).
                collect(Collectors.toSet())).
                orElse(Set.of());
    }

    public static Map<String, Set<String>> getAuthorizations() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()).
                map(authentication -> authentication.getAuthorities().stream().
                filter(SyncopeGrantedAuthority.class::isInstance).
                map(SyncopeGrantedAuthority.class::cast).
                collect(Collectors.toMap(SyncopeGrantedAuthority::getAuthority, SyncopeGrantedAuthority::getRealms))).
                orElse(Map.of());
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

    private static <T> T call(final String domain, final Authentication fakeAuth, final Callable<T> callable) {
        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(fakeAuth);
        try {
            return callable.call();
        } catch (Exception e) {
            LOG.debug("Error during execution with domain {} context", domain, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(original);
        }
    }

    public static <T> T callAs(
            final String domain,
            final String username,
            final Collection<String> entitlements,
            final Callable<T> callable) {

        List<GrantedAuthority> authorities = entitlements.stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());
        UsernamePasswordAuthenticationToken fakeAuth = new UsernamePasswordAuthenticationToken(
                new User(username, FAKE_PASSWORD, authorities), FAKE_PASSWORD, authorities);
        fakeAuth.setDetails(new SyncopeAuthenticationDetails(domain, getDelegatedBy().orElse(null)));

        return call(domain, fakeAuth, callable);
    }

    public static <T> T callAsAdmin(final String domain, final Callable<T> callable) {
        SecurityProperties properties = ApplicationContextProvider.getBeanFactory().getBean(SecurityProperties.class);
        return callAs(
                domain,
                properties.getAdminUser(),
                EntitlementsHolder.getInstance().getValues(),
                callable);
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private AuthContextUtils() {
    }
}
