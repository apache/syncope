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

import org.apache.syncope.sra.security.pac4j.ServerHttpContext;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import reactor.core.publisher.Mono;

public class SAML2ServerLogoutHandler extends SAML2RequestGenerator implements ServerLogoutHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2ServerLogoutHandler.class);

    public SAML2ServerLogoutHandler(final SAML2Client saml2Client) {
        super(saml2Client);
    }

    @Override
    public Mono<Void> logout(final WebFilterExchange exchange, final Authentication authentication) {
        return exchange.getExchange().getSession().
                flatMap(session -> {
                    SAML2Credentials credentials = (SAML2Credentials) authentication.getPrincipal();

                    LOG.debug("Creating SAML2 SP Logout Request for IDP[{}] and Profile[{}]",
                            saml2Client.getIdentityProviderResolvedEntityId(), credentials.getUserProfile());

                    ServerHttpContext shc = new ServerHttpContext(exchange.getExchange(), session);

                    return session.invalidate().then(
                            saml2Client.getLogoutAction(shc, credentials.getUserProfile(), null).
                                    map(action -> handle(action, shc)).
                                    orElseThrow(() -> new IllegalStateException("No action generated")));
                }).onErrorResume(Mono::error);
    }
}
