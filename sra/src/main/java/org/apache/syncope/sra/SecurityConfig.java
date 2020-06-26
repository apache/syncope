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
package org.apache.syncope.sra;

import java.util.Objects;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.sra.security.CsrfRouteMatcher;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.apache.syncope.sra.security.OAuth2SecurityConfigUtils;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

    private static final String AM_TYPE = "am.type";

    public enum AMType {
        OIDC,
        OAUTH2,
        SAML2,
        WA

    }

    @Autowired
    private Environment env;

    @Bean
    @Order(0)
    public SecurityWebFilterChain actuatorSecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher actuatorMatcher = EndpointRequest.toAnyEndpoint();
        return http.securityMatcher(actuatorMatcher).
                authorizeExchange().anyExchange().authenticated().
                and().httpBasic().
                and().csrf().requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(actuatorMatcher)).
                and().build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder().
                username(Objects.requireNonNull(env.getProperty("anonymousUser"))).
                password("{noop}" + env.getProperty("anonymousKey")).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OIDC")
    public ReactiveClientRegistrationRepository oidcClientRegistrationRepository() {
        return new InMemoryReactiveClientRegistrationRepository(
                ClientRegistrations.fromOidcIssuerLocation(env.getProperty("am.oidc.configuration")).
                        registrationId("OIDC").
                        clientId(env.getProperty("am.oidc.client.id")).
                        clientSecret(env.getProperty("am.oidc.client.secret")).
                        build());
    }

    @Bean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OAUTH2")
    public ReactiveClientRegistrationRepository oauth2ClientRegistrationRepository() {
        return new InMemoryReactiveClientRegistrationRepository(
                ClientRegistration.withRegistrationId("OAUTH2").
                        redirectUriTemplate("{baseUrl}/{action}/oauth2/code/{registrationId}").
                        tokenUri(env.getProperty("am.oauth2.tokenUri")).
                        authorizationUri(env.getProperty("am.oauth2.authorizationUri")).
                        userInfoUri(env.getProperty("am.oauth2.userInfoUri")).
                        userNameAttributeName(env.getProperty("am.oauth2.userNameAttributeName")).
                        clientId(env.getProperty("am.oauth2.client.id")).
                        clientSecret(env.getProperty("am.oauth2.client.secret")).
                        scope(env.getProperty("am.oauth2.scopes", String[].class)).
                        authorizationGrantType(new AuthorizationGrantType(env.getProperty("am.oauth2.grantType"))).
                        build());
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = AM_TYPE)
    public SecurityWebFilterChain routesSecurityFilterChain(
            final ServerHttpSecurity http,
            final CacheManager cacheManager,
            final LogoutRouteMatcher logoutRouteMatcher,
            final PublicRouteMatcher publicRouteMatcher,
            final CsrfRouteMatcher csrfRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        AMType amType = AMType.valueOf(env.getProperty(AM_TYPE));

        ServerHttpSecurity.AuthorizeExchangeSpec builder = http.authorizeExchange().
                matchers(publicRouteMatcher).permitAll().
                anyExchange().authenticated();

        switch (amType) {
            case OIDC:
            case OAUTH2:
                OAuth2SecurityConfigUtils.forLogin(http, amType, ctx);
                OAuth2SecurityConfigUtils.forLogout(builder, amType, cacheManager, logoutRouteMatcher, ctx);
                break;

            case SAML2:
                break;

            case WA:
            default:
        }

        return builder.and().csrf().requireCsrfProtectionMatcher(csrfRouteMatcher).and().build();
    }
}
