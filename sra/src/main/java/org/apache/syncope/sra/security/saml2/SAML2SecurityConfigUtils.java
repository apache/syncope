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
package org.apache.syncope.sra.security.saml2;

import org.apache.syncope.sra.ApplicationContextUtils;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.pac4j.saml.client.SAML2Client;
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
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

public final class SAML2SecurityConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2SecurityConfigUtils.class);

    private static ReactiveAuthenticationManager authenticationManager() {
        return authentication -> Mono.just(authentication).filter(Authentication::isAuthenticated);
    }

    public static void forLogin(
            final ServerHttpSecurity http,
            final SAML2Client saml2Client,
            final PublicRouteMatcher publicRouteMatcher) {

        ReactiveAuthenticationManager authenticationManager = authenticationManager();

        SAML2WebSsoAuthenticationRequestWebFilter authRequestFilter =
                new SAML2WebSsoAuthenticationRequestWebFilter(saml2Client);
        http.addFilterAt(authRequestFilter, SecurityWebFiltersOrder.HTTP_BASIC);

        AuthenticationWebFilter authenticationFilter =
                new SAML2WebSsoAuthenticationWebFilter(authenticationManager, saml2Client);
        authenticationFilter.setAuthenticationFailureHandler((exchange, ex) -> Mono.error(ex));
        authenticationFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());
        http.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        WebFilter anonymousRedirectFilter = new SAML2AnonymousWebFilter(publicRouteMatcher);
        http.addFilterAt(anonymousRedirectFilter, SecurityWebFiltersOrder.AUTHENTICATION);
    }

    public static void forLogout(
            final ServerHttpSecurity http,
            final SAML2Client saml2Client,
            final CacheManager cacheManager,
            final LogoutRouteMatcher logoutRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        LogoutWebFilter logoutWebFilter = new LogoutWebFilter();
        logoutWebFilter.setRequiresLogoutMatcher(logoutRouteMatcher);
        logoutWebFilter.setLogoutHandler(new SAML2RequestServerLogoutHandler(saml2Client, cacheManager));
        logoutWebFilter.setLogoutSuccessHandler((exchange, authentication) -> Mono.empty());

        try {
            SAML2ServerLogoutSuccessHandler logoutSuccessHandler = ApplicationContextUtils.getOrCreateBean(
                    ctx,
                    SAML2ServerLogoutSuccessHandler.class.getName(),
                    SAML2ServerLogoutSuccessHandler.class);

            SAML2LogoutResponseWebFilter logoutResponseWebFilter =
                    new SAML2LogoutResponseWebFilter(saml2Client, logoutSuccessHandler);
            http.addFilterAt(logoutResponseWebFilter, SecurityWebFiltersOrder.LOGOUT);
        } catch (ClassNotFoundException e) {
            LOG.error("While creating instance of {}", SAML2ServerLogoutSuccessHandler.class.getName(), e);
        }

        http.addFilterAt(logoutWebFilter, SecurityWebFiltersOrder.LOGOUT);
    }

    private SAML2SecurityConfigUtils() {
        // private constructor for static utility class
    }
}
