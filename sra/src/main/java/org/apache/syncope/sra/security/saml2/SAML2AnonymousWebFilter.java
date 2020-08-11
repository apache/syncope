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
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.apache.syncope.sra.session.SessionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class SAML2AnonymousWebFilter implements WebFilter {

    private final ServerWebExchangeMatcher matcher;

    public SAML2AnonymousWebFilter(final PublicRouteMatcher publicRouteMatcher) {
        this.matcher = ServerWebExchangeMatchers.matchers(
                publicRouteMatcher,
                SessionUtils.authInSession(),
                SAML2LogoutResponseWebFilter.MATCHER);
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return matcher.matches(exchange).
                filter(matchResult -> !matchResult.isMatch()).
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(r -> exchange.getSession()).
                flatMap(session -> {
                    session.getAttributes().put(SessionUtils.INITIAL_REQUEST_URI, exchange.getRequest().getURI());

                    exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                    exchange.getResponse().getHeaders().
                            setLocation(URI.create(SAML2WebSsoAuthenticationRequestWebFilter.AUTHENTICATE_URL));
                    return exchange.getResponse().setComplete();
                });
    }
}
