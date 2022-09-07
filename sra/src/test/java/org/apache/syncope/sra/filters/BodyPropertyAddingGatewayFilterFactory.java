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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.zookeeper.common.IOUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Inspired by {@link org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory}.
 */
public class BodyPropertyAddingGatewayFilterFactory extends CustomGatewayFilterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(BodyPropertyAddingGatewayFilterFactory.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static boolean isCompressed(final byte[] bytes) {
        if ((bytes == null) || (bytes.length < 2)) {
            return false;
        } else {
            return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                    && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
        }
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return new ModifyResponseGatewayFilter(config);
    }

    public static class ModifyResponseGatewayFilter implements GatewayFilter, Ordered {

        private final Config config;

        public ModifyResponseGatewayFilter(final Config config) {
            this.config = config;
        }

        @Override
        public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
            return chain.filter(exchange.mutate().response(decorate(exchange)).build());
        }

        private ServerHttpResponse decorate(final ServerWebExchange exchange) {
            ServerHttpResponse originalResponse = exchange.getResponse();

            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            return new ServerHttpResponseDecorator(originalResponse) {

                @Override
                public Mono<Void> writeWith(final Publisher<? extends DataBuffer> body) {
                    return super.writeWith(Flux.from(body).buffer().map(dataBuffers -> {
                        ByteArrayOutputStream payload = new ByteArrayOutputStream();
                        dataBuffers.forEach(buffer -> {
                            byte[] array = new byte[buffer.readableByteCount()];
                            buffer.read(array);
                            try {
                                payload.write(array);
                            } catch (IOException e) {
                                LOG.error("While reading original body content", e);
                            }
                        });

                        byte[] input = payload.toByteArray();

                        InputStream is = null;
                        boolean compressed = false;
                        byte[] output;
                        try {
                            if (isCompressed(input)) {
                                compressed = true;
                                is = new GZIPInputStream(new ByteArrayInputStream(input));
                            } else {
                                is = new ByteArrayInputStream(input);
                            }

                            ObjectNode content = (ObjectNode) MAPPER.readTree(is);
                            String[] kv = config.getData().split("=");
                            content.put(kv[0], kv[1]);

                            output = MAPPER.writeValueAsBytes(content);
                        } catch (IOException e) {
                            LOG.error("While (de)serializing as JSON", e);
                            output = ArrayUtils.clone(input);
                        } finally {
                            IOUtils.closeStream(is);
                        }

                        if (compressed) {
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(output.length)) {
                                try (GZIPOutputStream gzipos = new GZIPOutputStream(baos)) {
                                    gzipos.write(output);
                                }

                                output = baos.toByteArray();
                            } catch (IOException e) {
                                LOG.error("While GZIP-encoding output", e);
                            }
                        }

                        return bufferFactory.wrap(output);
                    }));
                }

                @Override
                public Mono<Void> writeAndFlushWith(final Publisher<? extends Publisher<? extends DataBuffer>> body) {
                    return writeWith(Flux.from(body).flatMapSequential(Function.identity()));
                }
            };
        }

        @Override
        public int getOrder() {
            return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
        }
    }
}
