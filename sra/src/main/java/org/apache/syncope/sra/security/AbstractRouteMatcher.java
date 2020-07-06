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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.sra.RouteProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public abstract class AbstractRouteMatcher
        implements ServerWebExchangeMatcher, ApplicationListener<RefreshRoutesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRouteMatcher.class);

    protected static final Map<String, Map<String, Boolean>> CACHE = new ConcurrentHashMap<>();

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    protected RouteProvider routeProvider;

    protected abstract String getCacheName();

    protected abstract boolean routeBehavior(Route route);

    @Override
    public void onApplicationEvent(final RefreshRoutesEvent event) {
        Optional.ofNullable(CACHE.get(getCacheName())).ifPresent(Map::clear);
    }

    @Override
    public Mono<MatchResult> matches(final ServerWebExchange exchange) {
        // see org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping#lookupRoute
        return routeLocator.getRoutes().
                // individually filter routes so that filterWhen error delaying is not a problem
                concatMap(route -> Mono.just(route).filterWhen(r -> r.getPredicate().apply(exchange)).
                // instead of immediately stopping main flux due to error, log and swallow it
                doOnError(e -> LOG.error("Error applying predicate for route: {}", route.getId(), e)).
                onErrorResume(e -> Mono.empty())).
                next().
                flatMap(route -> {
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR, route.getId());
                    LOG.debug("[{}] Route found: {}", getClass().getName(), route);

                    boolean cond = Optional.ofNullable(CACHE.get(getCacheName()).get(route.getId())).orElseGet(() -> {
                        boolean result = routeBehavior(route);
                        CACHE.get(getCacheName()).put(route.getId(), result);
                        return result;
                    });
                    LOG.debug("[{}] Condition matched: {}", getClass().getName(), cond);

                    return cond ? MatchResult.match() : MatchResult.notMatch();
                }).switchIfEmpty(Mono.defer(() -> {
            LOG.debug("[{}] No Route found", getClass().getName());
            return MatchResult.notMatch();
        }));
    }
}
