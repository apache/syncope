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
package org.apache.syncope.sra.security.cas;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.apache.syncope.sra.security.web.server.DoNothingIfCommittedServerRedirectStrategy;
import org.apache.syncope.sra.session.SessionUtils;
import org.apereo.cas.client.Protocol;
import org.apereo.cas.client.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class CASAuthenticationRequestWebFilter implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CASAuthenticationRequestWebFilter.class);

    private final ServerWebExchangeMatcher matcher;

    private final Protocol protocol;

    /**
     * The URL to the CAS Server login.
     */
    private final String casServerLoginUrl;

    private ServerRedirectStrategy authenticationRedirectStrategy = new DoNothingIfCommittedServerRedirectStrategy();

    public CASAuthenticationRequestWebFilter(
            final PublicRouteMatcher publicRouteMatcher,
            final Protocol protocol,
            final String casServerUrlPrefix) {

        matcher = ServerWebExchangeMatchers.matchers(
                publicRouteMatcher,
                CASUtils.ticketAvailable(protocol),
                SessionUtils.authInSession());
        this.protocol = protocol;
        casServerLoginUrl = StringUtils.appendIfMissing(casServerUrlPrefix, "/") + "login";
    }

    public void setAuthenticationRedirectStrategy(final ServerRedirectStrategy authenticationRedirectStrategy) {
        this.authenticationRedirectStrategy = authenticationRedirectStrategy;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return matcher.matches(exchange).
                filter(matchResult -> !matchResult.isMatch()).
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(r -> exchange.getSession()).
                flatMap(session -> {
                    session.getAttributes().
                            put(SessionUtils.INITIAL_REQUEST_URI, exchange.getRequest().getURI());

                    LOG.debug("no ticket and no assertion found");

                    String serviceUrl = CASUtils.constructServiceUrl(exchange, protocol);
                    LOG.debug("Constructed service url: {}", serviceUrl);

                    String urlToRedirectTo = CommonUtils.constructRedirectUrl(
                            casServerLoginUrl,
                            protocol.getServiceParameterName(),
                            serviceUrl,
                            false,
                            false,
                            null);
                    LOG.debug("redirecting to \"{}\"", urlToRedirectTo);

                    return authenticationRedirectStrategy.sendRedirect(exchange, URI.create(urlToRedirectTo));
                });
    }
}
