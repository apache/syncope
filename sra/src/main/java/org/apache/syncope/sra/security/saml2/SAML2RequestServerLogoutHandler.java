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

import org.apache.syncope.sra.SessionConfig;
import org.apache.syncope.sra.security.pac4j.NoOpSessionStore;
import org.apache.syncope.sra.security.pac4j.ServerWebExchangeContext;
import org.apache.syncope.sra.security.pac4j.ServerWebExchangeHttpActionAdapter;
import org.pac4j.core.context.CallContext;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.credentials.SAML2AuthenticationCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import reactor.core.publisher.Mono;

public class SAML2RequestServerLogoutHandler implements ServerLogoutHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2RequestServerLogoutHandler.class);

    private final SAML2Client saml2Client;

    private final CacheManager cacheManager;

    public SAML2RequestServerLogoutHandler(final SAML2Client saml2Client, final CacheManager cacheManager) {
        this.saml2Client = saml2Client;
        this.cacheManager = cacheManager;
    }

    @Override
    public Mono<Void> logout(final WebFilterExchange exchange, final Authentication authentication) {
        return exchange.getExchange().getSession().
                flatMap(session -> {
                    SAML2AuthenticationCredentials credentials =
                            (SAML2AuthenticationCredentials) authentication.getPrincipal();

                    LOG.debug("Creating SAML2 SP Logout Request for IDP[{}] and Profile[{}]",
                            saml2Client.getIdentityProviderResolvedEntityId(), credentials.getUserProfile());

                    ServerWebExchangeContext swec = new ServerWebExchangeContext(exchange.getExchange());

                    cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evictIfPresent(session.getId());
                    return session.invalidate().then(saml2Client.getLogoutAction(
                            new CallContext(swec, NoOpSessionStore.INSTANCE), credentials.getUserProfile(), null).
                            map(action -> ServerWebExchangeHttpActionAdapter.INSTANCE.adapt(action, swec)).
                            orElseThrow(() -> new IllegalStateException("No action generated")));
                }).onErrorResume(Mono::error);
    }
}
