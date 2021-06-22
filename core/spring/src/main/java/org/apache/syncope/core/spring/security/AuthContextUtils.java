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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
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

    public static Optional<String> getDelegatedBy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null && auth.getDetails() instanceof SyncopeAuthenticationDetails
                ? Optional.ofNullable(SyncopeAuthenticationDetails.class.cast(auth.getDetails()).getDelegatedBy())
                : Optional.empty();
    }

    public static String getWho() {
        return getUsername() + getDelegatedBy().map(d -> {
            String delegatedBy = execWithAuthContext(getDomain(),
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
                orElse(Collections.emptySet());
    }

    public static Map<String, Set<String>> getAuthorizations() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()).
                map(authentication -> authentication.getAuthorities().stream().
                filter(SyncopeGrantedAuthority.class::isInstance).
                map(SyncopeGrantedAuthority.class::cast).
                collect(Collectors.toMap(SyncopeGrantedAuthority::getAuthority, SyncopeGrantedAuthority::getRealms))).
                orElse(Collections.emptyMap());
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

    private static Authentication getFakeAuth(final String domain) {
        List<GrantedAuthority> authorities = EntitlementsHolder.getInstance().getValues().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new User(ApplicationContextProvider.getBeanFactory().getBean("adminUser", String.class),
                        "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(domain, null));
        return auth;
    }

    public static <T> T execWithAuthContext(final String domain, final Executable<T> executable) {
        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(getFakeAuth(domain));
        try {
            return executable.exec();
        } catch (Throwable t) {
            LOG.debug("Error during execution with domain {} context", domain, t);
            throw t;
        } finally {
            SecurityContextHolder.getContext().setAuthentication(original);
        }
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private AuthContextUtils() {
    }
}
