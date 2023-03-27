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

import io.netty.channel.unix.Errors.NativeIoException;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Order(-2)
public class SyncopeSRAWebExceptionHandler implements WebExceptionHandler, ApplicationListener<RefreshRoutesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeSRAWebExceptionHandler.class);

    private static final Map<String, Optional<URI>> CACHE = new ConcurrentHashMap<>();

    private final RouteProvider routeProvider;

    private final SRAProperties props;

    public SyncopeSRAWebExceptionHandler(final RouteProvider routeProvider, final SRAProperties props) {
        this.routeProvider = routeProvider;
        this.props = props;
    }

    @Override
    public void onApplicationEvent(final RefreshRoutesEvent event) {
        CACHE.clear();
    }

    private URI getError(final ServerWebExchange exchange) {
        URI error = props.getGlobal().getError();
        String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR);
        if (StringUtils.isNotBlank(routeId)) {
            Optional<URI> routeError = Optional.ofNullable(CACHE.get(routeId)).orElseGet(() -> {
                Optional<SRARouteTO> route = routeProvider.getRouteTOs().stream().
                        filter(r -> routeId.equals(r.getKey())).findFirst();
                URI uri = route.map(SRARouteTO::getError).orElse(null);

                CACHE.put(routeId, Optional.ofNullable(uri));
                return CACHE.get(routeId);
            });
            if (routeError.isPresent()) {
                error = routeError.get();
            }
        }

        return error;
    }

    private boolean acceptsTextHtml(final ServerHttpRequest request) {
        try {
            List<MediaType> acceptedMediaTypes = request.getHeaders().getAccept();
            acceptedMediaTypes.remove(MediaType.ALL);
            MimeTypeUtils.sortBySpecificity(acceptedMediaTypes);
            return acceptedMediaTypes.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
        } catch (InvalidMediaTypeException e) {
            LOG.debug("Unexpected exception", e);
            return false;
        }
    }

    private Mono<Void> doHandle(final ServerWebExchange exchange, final Throwable throwable, final HttpStatus status) {
        try {
            if (acceptsTextHtml(exchange.getRequest())) {
                exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);

                URI error = getError(exchange);
                exchange.getResponse().getHeaders().add(HttpHeaders.LOCATION, error.toASCIIString());
            } else {
                exchange.getResponse().setStatusCode(status);

                exchange.getResponse().getHeaders().add(
                        RESTHeaders.ERROR_CODE, HttpStatus.NOT_FOUND.toString());
                exchange.getResponse().getHeaders().add(
                        RESTHeaders.ERROR_INFO, throwable.getMessage().replace("\n", " "));
            }
        } catch (UnsupportedOperationException e) {
            LOG.debug("Could not perform, ignoring", e);
        }

        return exchange.getResponse().setComplete();
    }

    @Override
    public Mono<Void> handle(final ServerWebExchange exchange, final Throwable throwable) {
        if (throwable instanceof ConnectException
                || throwable instanceof NativeIoException
                || throwable instanceof NotFoundException) {

            LOG.error("ConnectException thrown", throwable);

            return doHandle(exchange, throwable, HttpStatus.NOT_FOUND);
        } else if (throwable instanceof OAuth2AuthorizationException) {
            LOG.error("OAuth2AuthorizationException thrown", throwable);

            return doHandle(exchange, throwable, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return Mono.error(throwable);
    }
}
