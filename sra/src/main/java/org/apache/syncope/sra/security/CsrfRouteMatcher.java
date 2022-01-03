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
package org.apache.syncope.sra.security;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CsrfRouteMatcher extends AbstractRouteMatcher {

    private static final String CACHE_NAME = CsrfRouteMatcher.class.getName();

    private static final Set<HttpMethod> ALLOWED_METHODS = Set.of(
            HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.OPTIONS);

    static {
        CACHE.put(CACHE_NAME, new ConcurrentHashMap<>());
    }

    private final PublicRouteMatcher publicRouteMatcher;

    public CsrfRouteMatcher(final PublicRouteMatcher publicRouteMatcher) {
        this.publicRouteMatcher = publicRouteMatcher;
    }

    @Override
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    protected boolean routeBehavior(final Route route) {
        return routeProvider.getRouteTOs().stream().
                filter(r -> route.getId().equals(r.getKey())).findFirst().
                map(SRARouteTO::isCsrf).orElse(true)
                && !publicRouteMatcher.routeBehavior(route);
    }

    @Override
    public Mono<MatchResult> matches(final ServerWebExchange exchange) {
        return Mono.just(exchange.getRequest()).
                map(HttpRequest::getMethod).
                filter(ALLOWED_METHODS::contains).
                flatMap(m -> MatchResult.notMatch()).
                switchIfEmpty(super.matches(exchange));
    }
}
