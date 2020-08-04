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
import org.apache.syncope.sra.security.web.server.DoNothingIfCommittedServerRedirectStrategy;
import org.apache.syncope.sra.session.SessionUtils;
import org.jasig.cas.client.Protocol;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.jasig.cas.client.validation.json.Cas30JsonServiceTicketValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

public class CASAuthenticationWebFilter extends AuthenticationWebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CASAuthenticationWebFilter.class);

    private final String serverName;

    private final Protocol protocol;

    private final TicketValidator ticketValidator;

    public CASAuthenticationWebFilter(
            final ReactiveAuthenticationManager authenticationManager,
            final String serverName,
            final Protocol protocol,
            final String casServerUrlPrefix) {

        super(authenticationManager);

        this.serverName = serverName;
        this.protocol = protocol;
        this.ticketValidator = new Cas30JsonServiceTicketValidator(casServerUrlPrefix);

        setRequiresAuthenticationMatcher(new AndServerWebExchangeMatcher(
                CASUtils.ticketAvailable(protocol),
                new NegatedServerWebExchangeMatcher(SessionUtils.authInSession())));

        setServerAuthenticationConverter(validateAssertion());

        setAuthenticationSuccessHandler(redirectToInitialRequestURI());
    }

    private ServerAuthenticationConverter validateAssertion() {
        return exchange -> CASUtils.retrieveTicketFromRequest(exchange, this.protocol).
                flatMap(ticket -> {
                    try {
                        Assertion assertion = this.ticketValidator.validate(
                                ticket,
                                CASUtils.constructServiceUrl(exchange, this.serverName, this.protocol));
                        return Mono.just(new CASAuthenticationToken(assertion));
                    } catch (TicketValidationException e) {
                        LOG.error("Could not validate {}", ticket, e);
                        throw new BadCredentialsException("Could not validate " + ticket);
                    }
                });
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
