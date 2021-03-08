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

import java.net.URI;

import org.apache.syncope.sra.security.pac4j.NoOpSessionStore;
import org.apache.syncope.sra.security.pac4j.ServerWebExchangeContext;
import org.apache.syncope.sra.security.web.server.DoNothingIfCommittedServerRedirectStrategy;
import org.apache.syncope.sra.session.SessionUtils;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class SAML2WebSsoAuthenticationWebFilter extends AuthenticationWebFilter {

    public static final String FILTER_PROCESSES_URI = "/login/saml2/sso";

    private static final ServerWebExchangeMatcher MATCHER =
            ServerWebExchangeMatchers.pathMatchers(FILTER_PROCESSES_URI);

    private final SAML2Client saml2Client;

    public SAML2WebSsoAuthenticationWebFilter(
            final ReactiveAuthenticationManager authenticationManager,
            final SAML2Client saml2Client) {

        super(authenticationManager);

        this.saml2Client = saml2Client;

        setRequiresAuthenticationMatcher(matchSamlResponse());

        setServerAuthenticationConverter(convertSamlResponse());

        setAuthenticationSuccessHandler(redirectToInitialRequestURI());
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return super.filter(exchange, chain).then(Mono.defer(exchange.getResponse()::setComplete));
    }

    private ServerWebExchangeMatcher matchSamlResponse() {
        return exchange -> exchange.getFormData().
                filter(form -> form.containsKey("SAMLResponse")).
                flatMap(form -> ServerWebExchangeMatcher.MatchResult.match()).
                switchIfEmpty(ServerWebExchangeMatcher.MatchResult.notMatch());
    }

    private ServerAuthenticationConverter convertSamlResponse() {
        return exchange -> exchange.getFormData().
                flatMap(form -> MATCHER.matches(exchange).
                flatMap(matchResult -> {
                    ServerWebExchangeContext swec = new ServerWebExchangeContext(exchange).setForm(form);

                    SAML2Credentials credentials = (SAML2Credentials) saml2Client.getCredentialsExtractor().
                        extract(swec, NoOpSessionStore.INSTANCE).
                            orElseThrow(() -> new IllegalStateException("No AuthnResponse found"));

                    saml2Client.getAuthenticator().validate(credentials, swec, NoOpSessionStore.INSTANCE);

                    return Mono.just(new SAML2AuthenticationToken(credentials));
                }));
    }

    private ServerAuthenticationSuccessHandler redirectToInitialRequestURI() {
        return new ServerAuthenticationSuccessHandler() {

            private final ServerRedirectStrategy redirectStrategy = new DoNothingIfCommittedServerRedirectStrategy();

            @Override
            public Mono<Void> onAuthenticationSuccess(
                    final WebFilterExchange webFilterExchange, final Authentication authentication) {

                return webFilterExchange.getExchange().getSession().
                        flatMap(session -> this.redirectStrategy.sendRedirect(
                        webFilterExchange.getExchange(),
                        session.<URI>getRequiredAttribute(SessionUtils.INITIAL_REQUEST_URI)));
            }
        };
    }
}
