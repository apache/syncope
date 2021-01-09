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
package org.apache.syncope.sra.security.oauth2;

import org.apache.syncope.sra.SessionConfig;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

public class OAuth2SessionRemovalServerLogoutHandler implements ServerLogoutHandler {

    private final CacheManager cacheManager;

    public OAuth2SessionRemovalServerLogoutHandler(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Mono<Void> logout(final WebFilterExchange exchange, final Authentication authentication) {
        return exchange.getExchange().getSession().
                doOnNext(session -> cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evictIfPresent(session.getId())).
                flatMap(WebSession::invalidate);
    }
}
