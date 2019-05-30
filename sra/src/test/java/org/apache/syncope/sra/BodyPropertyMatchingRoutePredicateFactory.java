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
package org.apache.syncope.sra;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
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
                // Join all the DataBuffers so we have a single DataBuffer for the body
                return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
                    // Update the retain counts so we can read the body twice, once to parse into an object
                    // that we can test the predicate against and a second time when the HTTP client sends
                    // the request downstream 
                    // Note: if we end up reading the body twice we will run into a problem, but as of right
                    // now there is no good use case for doing this
                    DataBufferUtils.retain(dataBuffer);
                    // Make a slice for each read so each read has its own read/write indexes
                    Flux<DataBuffer> cachedFlux = Flux.defer(() -> Flux.just(
                            dataBuffer.slice(0, dataBuffer.readableByteCount())));

                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {

                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedFlux;
                        }
                    };
                    return ServerRequest.create(exchange.mutate().request(mutatedRequest).build(), MESSAGE_READERS).
                            bodyToMono(JsonNode.class).doOnNext(value -> {
                        exchange.getAttributes().put(CACHE_REQUEST_BODY_OBJECT_KEY, value);
                        exchange.getAttributes().put(AdaptCachedBodyGlobalFilter.CACHED_REQUEST_BODY_KEY, cachedFlux);
                    }).map(objectValue -> objectValue.has(config.getData()));
                });
            } else {
                return Mono.just(cachedBody.has(config.getData()));
            }
        };
    }
}
