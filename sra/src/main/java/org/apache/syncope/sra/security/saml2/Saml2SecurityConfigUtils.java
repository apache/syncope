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
import org.apache.syncope.sra.SecurityConfig;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.opensaml.security.credential.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlLogoutRequestFactory;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

public final class Saml2SecurityConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(Saml2SecurityConfigUtils.class);

    private static ReactiveAuthenticationManager authenticationManager() {
        OpenSamlAuthenticationProvider openSamlAuthenticationProvider = new OpenSamlAuthenticationProvider();
        return authentication -> Mono.just(authentication).
                flatMap(a -> {
                    try {
                        return Mono.just(openSamlAuthenticationProvider.authenticate(a));
                    } catch (Throwable error) {
                        return Mono.error(error);
                    }
                }).
                filter(Authentication::isAuthenticated);
    }

    public static void forLogin(
            final ServerHttpSecurity http,
            final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            final PublicRouteMatcher publicRouteMatcher) {

        ReactiveAuthenticationManager authenticationManager = authenticationManager();

        Saml2WebSsoAuthenticationRequestWebFilter authRequestFilter =
                new Saml2WebSsoAuthenticationRequestWebFilter(relyingPartyRegistrationRepository);
        http.addFilterAt(authRequestFilter, SecurityWebFiltersOrder.HTTP_BASIC);

        AuthenticationWebFilter authenticationFilter =
                new Saml2WebSsoAuthenticationWebFilter(authenticationManager, relyingPartyRegistrationRepository);
        authenticationFilter.setAuthenticationFailureHandler((exchange, ex) -> Mono.error(ex));
        authenticationFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());
        http.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        WebFilter anonymousRedirectFilter =
                new Saml2AnonymousWebFilter(publicRouteMatcher, SecurityConfig.AMType.SAML2.name());
        http.addFilterAt(anonymousRedirectFilter, SecurityWebFiltersOrder.AUTHENTICATION);
    }

    public static void forLogout(
            final ServerHttpSecurity.AuthorizeExchangeSpec builder,
            final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            final LogoutRouteMatcher logoutRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        LogoutWebFilter logoutWebFilter = new LogoutWebFilter();
        logoutWebFilter.setRequiresLogoutMatcher(logoutRouteMatcher);

        Saml2ServerLogoutHandler logoutHandler =
                new Saml2ServerLogoutHandler(relyingPartyRegistrationRepository, SecurityConfig.AMType.SAML2.name());
        logoutHandler.setLogoutRequestFactory(new OpenSamlLogoutRequestFactory(ctx.getBean(Credential.class)));
        logoutWebFilter.setLogoutHandler(logoutHandler);

        try {
            Saml2ServerLogoutSuccessHandler handler = ApplicationContextUtils.getOrCreateBean(
                    ctx,
                    Saml2ServerLogoutSuccessHandler.class.getName(),
                    Saml2ServerLogoutSuccessHandler.class);
            logoutWebFilter.setLogoutSuccessHandler(handler);
        } catch (ClassNotFoundException e) {
            LOG.error("While creating instance of {}",
                    Saml2ServerLogoutSuccessHandler.class.getName(), e);
        }

        builder.and().addFilterAt(logoutWebFilter, SecurityWebFiltersOrder.LOGOUT);
    }

    private Saml2SecurityConfigUtils() {
        // private constructor for static utility class
    }
}
