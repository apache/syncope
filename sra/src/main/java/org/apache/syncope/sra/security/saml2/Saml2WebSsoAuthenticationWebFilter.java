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
import java.nio.charset.StandardCharsets;
import org.apache.syncope.sra.security.web.server.DoNothingIfCommittedServerRedirectStrategy;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class Saml2WebSsoAuthenticationWebFilter extends AuthenticationWebFilter {

    private final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private ServerWebExchangeMatcher matcher =
            ServerWebExchangeMatchers.pathMatchers(Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI);

    public Saml2WebSsoAuthenticationWebFilter(
            final ReactiveAuthenticationManager authenticationManager,
            final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        super(authenticationManager);

        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;

        setRequiresAuthenticationMatcher(matchSamlResponse());

        setServerAuthenticationConverter(convertSamlResponse());

        setAuthenticationSuccessHandler(redirectToInitialRequestURI());
    }

    public void setMatcher(final ServerWebExchangeMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return super.filter(exchange, chain).then(Mono.defer(exchange.getResponse()::setComplete));
    }

    private ServerWebExchangeMatcher matchSamlResponse() {
        return exchange -> exchange.getFormData().
                filter(form -> form.containsKey(Saml2Constants.SAML_RESPONSE)).
                flatMap(form -> ServerWebExchangeMatcher.MatchResult.match()).
                switchIfEmpty(ServerWebExchangeMatcher.MatchResult.notMatch());
    }

    private ServerAuthenticationConverter convertSamlResponse() {
        return exchange -> exchange.getFormData().flatMap(form -> {
            String saml2Response = form.getFirst(Saml2Constants.SAML_RESPONSE);
            byte[] b = Saml2ReactiveUtils.samlDecode(saml2Response);

            String responseXml = inflateIfRequired(exchange.getRequest(), b);
            return this.matcher.matches(exchange).
                    flatMap(matchResult -> {
                        String registrationId = matchResult.getVariables().get("registrationId").toString();
                        return this.relyingPartyRegistrationRepository.findByRegistrationId(registrationId).
                                switchIfEmpty(Mono.error(() -> new Saml2AuthenticationException(
                                new Saml2Error(
                                        Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                                        "Relying Party Registration not found with ID: " + registrationId))));
                    }).
                    flatMap(rp -> {
                        String applicationUri = Saml2ReactiveUtils.getApplicationUri(exchange.getRequest());
                        String localSpEntityId = Saml2ReactiveUtils.resolveUrlTemplate(
                                rp.getLocalEntityIdTemplate(), applicationUri, rp);

                        Saml2AuthenticationToken authentication = new Saml2AuthenticationToken(
                                responseXml,
                                exchange.getRequest().getURI().toASCIIString(),
                                rp.getProviderDetails().getEntityId(),
                                localSpEntityId,
                                rp.getCredentials());
                        return Mono.just(authentication);
                    });
        });
    }

    private String inflateIfRequired(final ServerHttpRequest request, final byte[] b) {
        return HttpMethod.GET == request.getMethod()
                ? Saml2ReactiveUtils.samlInflate(b)
                : new String(b, StandardCharsets.UTF_8);
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
                        (URI) session.getRequiredAttribute(Saml2Constants.INITIAL_REQUEST_URI)));
            }
        };
    }
}
