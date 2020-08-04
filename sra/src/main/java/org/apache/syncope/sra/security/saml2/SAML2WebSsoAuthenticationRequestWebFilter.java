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

import org.apache.syncope.sra.security.pac4j.ServerWebExchangeContext;
import org.pac4j.saml.client.SAML2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class SAML2WebSsoAuthenticationRequestWebFilter extends SAML2RequestGenerator implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2WebSsoAuthenticationRequestWebFilter.class);

    public static final String AUTHENTICATE_URL = "/saml2/authenticate";

    private ServerWebExchangeMatcher redirectMatcher = ServerWebExchangeMatchers.pathMatchers(AUTHENTICATE_URL);

    public SAML2WebSsoAuthenticationRequestWebFilter(final SAML2Client saml2Client) {
        super(saml2Client);
    }

    public void setRedirectMatcher(final ServerWebExchangeMatcher redirectMatcher) {
        Assert.notNull(redirectMatcher, "redirectMatcher cannot be null");
        this.redirectMatcher = redirectMatcher;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return redirectMatcher.matches(exchange).
                filter(matchResult -> matchResult.isMatch()).
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(matchResult -> exchange.getSession()).
                flatMap(session -> {
                    LOG.debug("Creating SAML2 SP Authentication Request for IDP[{}]",
                            saml2Client.getIdentityProviderResolvedEntityId());

                    ServerWebExchangeContext swec = new ServerWebExchangeContext(exchange, session);

                    return saml2Client.getRedirectionAction(swec).
                            map(action -> handle(action, swec)).
                            orElseThrow(() -> new IllegalStateException("No action generated"));
                }).onErrorResume(Mono::error);
    }
}
