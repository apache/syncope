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
package org.apache.syncope.sra.security.cas;

import org.apache.syncope.sra.ApplicationContextUtils;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.apereo.cas.client.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import reactor.core.publisher.Mono;

public final class CASSecurityConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CASSecurityConfigUtils.class);

    private static ReactiveAuthenticationManager authenticationManager() {
        return authentication -> Mono.just(authentication).filter(Authentication::isAuthenticated);
    }

    public static void forLogin(
            final ServerHttpSecurity http,
            final Protocol protocol,
            final String casServerUrlPrefix,
            final PublicRouteMatcher publicRouteMatcher) {

        ReactiveAuthenticationManager authenticationManager = authenticationManager();

        CASAuthenticationRequestWebFilter authRequestFilter = new CASAuthenticationRequestWebFilter(
                publicRouteMatcher,
                protocol,
                casServerUrlPrefix);
        http.addFilterAt(authRequestFilter, SecurityWebFiltersOrder.HTTP_BASIC);

        AuthenticationWebFilter authenticationFilter = new CASAuthenticationWebFilter(
                authenticationManager,
                protocol,
                casServerUrlPrefix);
        authenticationFilter.setAuthenticationFailureHandler((exchange, ex) -> Mono.error(ex));
        authenticationFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());
        http.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);
    }

    public static void forLogout(
            final ServerHttpSecurity http,
            final CacheManager cacheManager,
            final String casServerUrlPrefix,
            final LogoutRouteMatcher logoutRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        LogoutWebFilter logoutWebFilter = new LogoutWebFilter();
        logoutWebFilter.setRequiresLogoutMatcher(logoutRouteMatcher);

        logoutWebFilter.setLogoutHandler(new CASServerLogoutHandler(cacheManager, casServerUrlPrefix));

        try {
            CASServerLogoutSuccessHandler handler = ApplicationContextUtils.getOrCreateBean(ctx,
                    CASServerLogoutSuccessHandler.class.getName(),
                    CASServerLogoutSuccessHandler.class);
            logoutWebFilter.setLogoutSuccessHandler(handler);
        } catch (ClassNotFoundException e) {
            LOG.error("While creating instance of {}", CASServerLogoutSuccessHandler.class.getName(), e);
        }

        http.addFilterAt(logoutWebFilter, SecurityWebFiltersOrder.LOGOUT);
    }

    private CASSecurityConfigUtils() {
        // private constructor for static utility class
    }
}
