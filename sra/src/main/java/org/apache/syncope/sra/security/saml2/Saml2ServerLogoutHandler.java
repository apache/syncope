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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

public class Saml2ServerLogoutHandler extends Saml2RequestGenerator implements ServerLogoutHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Saml2ServerLogoutHandler.class);

    private final String registrationId;

    public Saml2ServerLogoutHandler(
            final ReactiveRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            final String registrationId) {

        super(relyingPartyRegistrationRepository);
        this.registrationId = registrationId;
    }

    @Override
    public Mono<Void> logout(final WebFilterExchange exchange, final Authentication authentication) {
        return exchange.getExchange().getSession().
                doOnNext(WebSession::invalidate).
                then(relyingPartyRegistrationRepository.findExtendedByRegistrationId(registrationId).
                        switchIfEmpty(Mono.fromRunnable(
                                () -> exchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))).
                        filter(rp -> rp.getLogoutDetails() != null).
                        switchIfEmpty(exchange.getChain().filter(exchange.getExchange()).then(Mono.empty())).
                        flatMap(rp -> {
                            LOG.debug("Creating SAML2 SP Logout Request for IDP[{}]",
                                    rp.getRelyingPartyRegistration().getRegistrationId());

                            ServerHttpRequest request = exchange.getExchange().getRequest();
                            ServerHttpResponse response = exchange.getExchange().getResponse();

                            String issuer = UriComponentsBuilder.fromHttpRequest(request).
                                    replacePath(Saml2MetadataEndpoint.METADATA_URL).toUriString();

                            return rp.getLogoutDetails().getBinding() == Saml2MessageBinding.REDIRECT
                                    ? sendRedirect(
                                            response,
                                            issuer,
                                            rp,
                                            request.getQueryParams().getFirst(Saml2Constants.RELAY_STATE))
                                    : sendPost(
                                            response,
                                            issuer,
                                            rp);
                        }));
    }
}
