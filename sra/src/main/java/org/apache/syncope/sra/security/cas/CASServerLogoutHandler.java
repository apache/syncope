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
import org.apache.syncope.sra.SessionConfig;
import org.apache.syncope.sra.security.web.server.DoNothingIfCommittedServerRedirectStrategy;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import reactor.core.publisher.Mono;

public class CASServerLogoutHandler implements ServerLogoutHandler {

    private final ServerRedirectStrategy redirectStrategy = new DoNothingIfCommittedServerRedirectStrategy();

    private final CacheManager cacheManager;

    /**
     * The URL to the CAS Server logout.
     */
    private final String casServerLogoutUrl;

    public CASServerLogoutHandler(final CacheManager cacheManager, final String casServerUrlPrefix) {
        this.cacheManager = cacheManager;
        this.casServerLogoutUrl = StringUtils.appendIfMissing(casServerUrlPrefix, "/") + "logout";
    }

    @Override
    public Mono<Void> logout(final WebFilterExchange exchange, final Authentication authentication) {
        return exchange.getExchange().getSession().
                flatMap(session -> {
                    cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evictIfPresent(session.getId());

                    return session.invalidate().then(
                            redirectStrategy.sendRedirect(exchange.getExchange(), URI.create(this.casServerLogoutUrl)));
                }).onErrorResume(Mono::error);
    }
}
