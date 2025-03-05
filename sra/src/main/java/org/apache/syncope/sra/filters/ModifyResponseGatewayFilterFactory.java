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
package org.apache.syncope.sra.filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Inspired by {@link org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory}.
 */
public abstract class ModifyResponseGatewayFilterFactory extends CustomGatewayFilterFactory {

    protected static final Logger LOG = LoggerFactory.getLogger(ModifyResponseGatewayFilterFactory.class);

    @Override
    public GatewayFilter apply(final Config config) {
        return new InternalModifyResponseGatewayFilter(config);
    }

    protected abstract byte[] modifyResponse(
            InputStream responseBody,
            Config config,
            ServerHttpResponseDecorator decorator,
            ServerWebExchange exchange)
            throws IOException;

    protected boolean skipCond(final ServerHttpResponseDecorator decorator) {
        LOG.debug("Decorator: {}", decorator);
        return false;
    }

    protected class InternalModifyResponseGatewayFilter implements GatewayFilter, Ordered {

        private final Config config;

        public InternalModifyResponseGatewayFilter(final Config config) {
            this.config = config;
        }

        @Override
        public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
            return chain.filter(exchange.mutate().response(decorate(exchange)).build());
        }

        private ServerHttpResponse decorate(final ServerWebExchange exchange) {
            return new ServerHttpResponseDecorator(exchange.getResponse()) {

                @Override
                public Mono<Void> writeWith(final Publisher<? extends DataBuffer> body) {
                    return skipCond(this)
                            ? super.writeWith(body)
                            : super.writeWith(Flux.from(body).
                                    collectList().
                                    filter(list -> !list.isEmpty()).
                                    map(list -> list.getFirst().factory().join(list)).
                                    doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release).
                                    map(dataBuffer -> {
                                        if (dataBuffer.readableByteCount() > 0) {
                                            LOG.trace("Retaining body in exchange attribute");
                                            exchange.getAttributes().put(
                                                    ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, dataBuffer);
                                        }

                                        boolean inputCompressed = false;
                                        if (dataBuffer.readableByteCount() >= 2) {
                                            byte[] first2 = new byte[2];
                                            dataBuffer.read(first2, 0, 2);
                                            dataBuffer.readPosition(0);

                                            inputCompressed = ((first2[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                                                    && (first2[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
                                        }

                                        boolean outputCompressed = false;
                                        byte[] output;
                                        try (InputStream is = inputCompressed
                                                ? new GZIPInputStream(dataBuffer.asInputStream())
                                                : dataBuffer.asInputStream()) {

                                            outputCompressed = is instanceof GZIPInputStream;

                                            output = modifyResponse(is, config, this, exchange);
                                        } catch (IOException e) {
                                            LOG.error("While modifying response", e);

                                            output = new byte[dataBuffer.readableByteCount()];
                                            dataBuffer.read(output);
                                        }

                                        if (outputCompressed) {
                                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(output.length);
                                                    GZIPOutputStream gzipos = new GZIPOutputStream(baos)) {

                                                gzipos.write(output);
                                                gzipos.finish();
                                                output = baos.toByteArray();
                                            } catch (IOException e) {
                                                LOG.error("While GZIP-encoding output", e);
                                            }
                                        }

                                        return exchange.getResponse().bufferFactory().wrap(output);
                                    }));
                }

                @Override
                public Mono<Void> writeAndFlushWith(final Publisher<? extends Publisher<? extends DataBuffer>> body) {
                    return writeWith(Flux.from(body).flatMapSequential(p -> p));
                }
            };
        }

        @Override
        public int getOrder() {
            return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
        }
    }
}
