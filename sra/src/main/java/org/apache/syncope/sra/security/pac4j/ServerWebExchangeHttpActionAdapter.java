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
package org.apache.syncope.sra.security.pac4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

public class ServerWebExchangeHttpActionAdapter implements HttpActionAdapter {

    public static final ServerWebExchangeHttpActionAdapter INSTANCE = new ServerWebExchangeHttpActionAdapter();

    @Override
    public Mono<Void> adapt(final HttpAction action, final WebContext context) {
        if (action == null) {
            throw new TechnicalException("No action provided");
        }

        ServerHttpResponse response = ((ServerWebExchangeContext) context).getNative().getResponse();

        switch (action) {
            case WithLocationAction withLocationAction -> {
                response.setStatusCode(HttpStatus.FOUND);
                response.getHeaders().setLocation(URI.create(withLocationAction.getLocation()));
                return response.setComplete();
            }

            case WithContentAction withContentAction -> {
                String content = Optional.ofNullable(withContentAction.getContent()).
                        orElseThrow(() -> new TechnicalException("No content set for POST AuthnRequest"));

                return Mono.defer(() -> {
                    response.getHeaders().setContentType(MediaType.TEXT_HTML);
                    return response.writeWith(Mono.just(
                            response.bufferFactory().wrap(content.getBytes(StandardCharsets.UTF_8))));
                });
            }

            default -> {
                throw new TechnicalException("Unsupported Action: " + action.getClass().getName());
            }
        }
    }
}
