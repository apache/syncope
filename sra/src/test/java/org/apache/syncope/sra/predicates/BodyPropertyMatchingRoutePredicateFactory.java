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
package org.apache.syncope.sra.predicates;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Inspired by {@link org.springframework.cloud.gateway.handler.predicate.ReadBodyPredicateFactory}.
 */
public class BodyPropertyMatchingRoutePredicateFactory extends CustomRoutePredicateFactory {

    private static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";

    private static final List<HttpMessageReader<?>> MESSAGE_READERS =
            HandlerStrategies.withDefaults().messageReaders();

    @Override
    public AsyncPredicate<ServerWebExchange> applyAsync(final Config config) {
        return exchange -> {
            JsonNode cachedBody = exchange.getAttribute(CACHE_REQUEST_BODY_OBJECT_KEY);
            if (cachedBody == null) {
                return ServerWebExchangeUtils.cacheRequestBodyAndRequest(
                        exchange, serverHttpRequest -> ServerRequest.create(
                                exchange.mutate().request(serverHttpRequest).build(), MESSAGE_READERS).
                                bodyToMono(JsonNode.class).
                                doOnNext(objectValue -> exchange.getAttributes().
                                put(CACHE_REQUEST_BODY_OBJECT_KEY, objectValue)).
                                map(objectValue -> objectValue.has(config.getData())));
            } else {
                return Mono.just(cachedBody.has(config.getData()));
            }
        };
    }
}
