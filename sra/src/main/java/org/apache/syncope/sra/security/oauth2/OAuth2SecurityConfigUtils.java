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
package org.apache.syncope.sra.security.oauth2;

import java.util.Set;
import org.apache.syncope.sra.ApplicationContextUtils;
import org.apache.syncope.sra.SRAProperties;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.OAuth2AuthorizationRequestRedirectWebFilter;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.authentication.OAuth2LoginAuthenticationWebFilter;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint.DelegateEntry;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.MediaTypeServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

public final class OAuth2SecurityConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2SecurityConfigUtils.class);

    private static ReactiveAuthenticationManager authenticationManager(final SRAProperties.AMType amType) {
        WebClientReactiveAuthorizationCodeTokenResponseClient client =
                new WebClientReactiveAuthorizationCodeTokenResponseClient();
        ReactiveAuthenticationManager authenticationManager =
                new OAuth2LoginReactiveAuthenticationManager(client, new DefaultReactiveOAuth2UserService());

        if (SRAProperties.AMType.OIDC == amType) {
            OidcAuthorizationCodeReactiveAuthenticationManager oidc =
                    new OidcAuthorizationCodeReactiveAuthenticationManager(client, new OidcReactiveOAuth2UserService());
            authenticationManager = new DelegatingReactiveAuthenticationManager(oidc, authenticationManager);
        }

        return authenticationManager;
    }

    public static void forLogin(
            final ServerHttpSecurity http,
            final SRAProperties.AMType amType,
            final ApplicationContext ctx) {

        ReactiveClientRegistrationRepository clientRegistrationRepository =
                ctx.getBean(ReactiveClientRegistrationRepository.class);

        ReactiveOAuth2AuthorizedClientService authorizedClientService =
                new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
        ServerOAuth2AuthorizedClientRepository authorizedClientRepository =
                new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService);

        OAuth2AuthorizationRequestRedirectWebFilter authRequestRedirectFilter =
                new OAuth2AuthorizationRequestRedirectWebFilter(clientRegistrationRepository);
        http.addFilterAt(authRequestRedirectFilter, SecurityWebFiltersOrder.HTTP_BASIC);

        AuthenticationWebFilter authenticationFilter =
                new OAuth2LoginAuthenticationWebFilter(authenticationManager(amType), authorizedClientRepository);
        authenticationFilter.setRequiresAuthenticationMatcher(
                new PathPatternParserServerWebExchangeMatcher("/login/oauth2/code/{registrationId}"));
        authenticationFilter.setServerAuthenticationConverter(
                new ServerOAuth2AuthorizationCodeAuthenticationTokenConverter(clientRegistrationRepository));
        authenticationFilter.setAuthenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler());
        authenticationFilter.setAuthenticationFailureHandler((exchange, ex) -> Mono.error(ex));
        authenticationFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());
        http.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        MediaTypeServerWebExchangeMatcher htmlMatcher = new MediaTypeServerWebExchangeMatcher(MediaType.TEXT_HTML);
        htmlMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        ServerAuthenticationEntryPoint entrypoint =
                new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/" + amType.name());
        http.exceptionHandling(customizer -> customizer.authenticationEntryPoint(
                new DelegateEntry(htmlMatcher, entrypoint).getEntryPoint()));
    }

    public static void forLogout(
            final ServerHttpSecurity http,
            final SRAProperties.AMType amType,
            final CacheManager cacheManager,
            final LogoutRouteMatcher logoutRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        LogoutWebFilter logoutWebFilter = new LogoutWebFilter();
        logoutWebFilter.setRequiresLogoutMatcher(logoutRouteMatcher);
        logoutWebFilter.setLogoutHandler(new OAuth2SessionRemovalServerLogoutHandler(cacheManager));

        if (SRAProperties.AMType.OIDC == amType) {
            try {
                OidcClientInitiatedServerLogoutSuccessHandler handler = ApplicationContextUtils.getOrCreateBean(
                        ctx,
                        OidcClientInitiatedServerLogoutSuccessHandler.class.getName(),
                        OidcClientInitiatedServerLogoutSuccessHandler.class);
                logoutWebFilter.setLogoutSuccessHandler(handler);
            } catch (ClassNotFoundException e) {
                LOG.error("While creating instance of {}",
                        OidcClientInitiatedServerLogoutSuccessHandler.class.getName(), e);
            }
        }

        http.logout(ServerHttpSecurity.LogoutSpec::disable);
        http.addFilterAt(logoutWebFilter, SecurityWebFiltersOrder.LOGOUT);
    }

    private OAuth2SecurityConfigUtils() {
        // private constructor for static utility class
    }
}
