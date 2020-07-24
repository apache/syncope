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
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class Saml2AnonymousWebFilter implements WebFilter {

    private final PublicRouteMatcher publicRouteMatcher;

    private final String registrationId;

    public Saml2AnonymousWebFilter(final PublicRouteMatcher publicRouteMatcher, final String registrationId) {
        this.publicRouteMatcher = publicRouteMatcher;
        this.registrationId = registrationId;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return publicRouteMatcher.matches(exchange).
                filter(matchResult -> !matchResult.isMatch()).
                flatMap(r -> exchange.getSession()).flatMap(r -> exchange.getSession()).
                filter(s -> !s.getAttributes().containsKey(
                WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME)).
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(session -> {
                    session.getAttributes().put(Saml2Constants.INITIAL_REQUEST_URI, exchange.getRequest().getURI());

                    exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                    exchange.getResponse().getHeaders().
                            setLocation(URI.create("/saml2/authenticate/" + registrationId));
                    return exchange.getResponse().setComplete();
                });
    }
}
