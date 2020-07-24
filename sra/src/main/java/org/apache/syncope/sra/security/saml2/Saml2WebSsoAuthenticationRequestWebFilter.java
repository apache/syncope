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

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class Saml2WebSsoAuthenticationRequestWebFilter extends Saml2RequestGenerator implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(Saml2WebSsoAuthenticationRequestWebFilter.class);

    private ServerWebExchangeMatcher redirectMatcher =
            ServerWebExchangeMatchers.pathMatchers("/saml2/authenticate/{registrationId}");

    public Saml2WebSsoAuthenticationRequestWebFilter(
            final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        super(relyingPartyRegistrationRepository);
    }

    public void setRedirectMatcher(final ServerWebExchangeMatcher redirectMatcher) {
        Assert.notNull(redirectMatcher, "redirectMatcher cannot be null");
        this.redirectMatcher = redirectMatcher;
    }

    private Saml2AuthenticationRequestContext createAuthenticationRequestContext(
            final RelyingPartyRegistration relyingParty,
            final ServerHttpRequest request) {

        String applicationUri = Saml2ReactiveUtils.getApplicationUri(request);
        Function<String, String> resolver =
                template -> Saml2ReactiveUtils.resolveUrlTemplate(template, applicationUri, relyingParty);
        String localSpEntityId = resolver.apply(relyingParty.getLocalEntityIdTemplate());
        String assertionConsumerServiceUrl = resolver.apply(relyingParty.getAssertionConsumerServiceUrlTemplate());
        return Saml2AuthenticationRequestContext.builder().
                issuer(localSpEntityId).
                relyingPartyRegistration(relyingParty).
                assertionConsumerServiceUrl(assertionConsumerServiceUrl).
                relayState(request.getQueryParams().getFirst(Saml2Constants.RELAY_STATE)).
                build();
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return redirectMatcher.matches(exchange).
                filter(matchResult -> matchResult.isMatch()).
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(matchResult -> {
                    String registrationId = matchResult.getVariables().get("registrationId").toString();
                    return this.relyingPartyRegistrationRepository.findByRegistrationId(registrationId);
                }).
                switchIfEmpty(Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))).
                flatMap(rp -> {
                    LOG.debug("Creating SAML2 SP Authentication Request for IDP[{}]", rp.getRegistrationId());

                    Saml2AuthenticationRequestContext authnRequestCtx =
                            createAuthenticationRequestContext(rp, exchange.getRequest());
                    return rp.getProviderDetails().getBinding() == Saml2MessageBinding.REDIRECT
                            ? sendRedirect(exchange.getResponse(), authnRequestCtx)
                            : sendPost(exchange.getResponse(), authnRequestCtx);
                });
    }
}
