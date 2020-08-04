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

import java.util.List;
import org.jasig.cas.client.Protocol;
import org.jasig.cas.client.util.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

public final class CASUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CASUtils.class);

    public static Mono<String> safeGetParameter(final ServerWebExchange exchange, final String parameter) {
        if (exchange.getRequest().getMethod() == HttpMethod.POST) {
            LOG.debug(
                    "safeGetParameter called on a POST ServerHttpRequest for Restricted Parameters. "
                    + "Cannot complete check safely. "
                    + "Reverting to standard behavior for this Parameter");
            return exchange.getFormData().
                    flatMap(form -> Mono.justOrEmpty(form.getFirst(parameter)));
        }
        return Mono.justOrEmpty(exchange.getRequest().getQueryParams().getFirst(parameter));
    }

    public static Mono<String> retrieveTicketFromRequest(final ServerWebExchange exchange, final Protocol protocol) {
        return safeGetParameter(exchange, protocol.getArtifactParameterName());
    }

    public static ServerWebExchangeMatcher ticketAvailable(final Protocol protocol) {
        return exchange -> CASUtils.retrieveTicketFromRequest(exchange, protocol).
                flatMap(ticket -> ServerWebExchangeMatcher.MatchResult.match()).
                switchIfEmpty(ServerWebExchangeMatcher.MatchResult.notMatch());
    }

    public static String constructServiceUrl(
            final ServerWebExchange exchange,
            final String serverName,
            final Protocol protocol) {

        UriComponents requestURI = UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build();

        URIBuilder originalRequestUrl = new URIBuilder(requestURI.toUriString(), false);
        originalRequestUrl.setParameters(requestURI.getQuery());

        URIBuilder builder;
        if (!serverName.startsWith("https://") && !serverName.startsWith("http://")) {
            String scheme = exchange.getRequest().getSslInfo() == null ? "http://" : "https://";
            builder = new URIBuilder(scheme + serverName, false);
        } else {
            builder = new URIBuilder(serverName, false);
        }

        builder.setPort(requestURI.getPort());

        builder.setEncodedPath(builder.getEncodedPath() + requestURI.getPath());

        List<String> serviceParameterNames = List.of(protocol.getServiceParameterName().split(","));
        if (!serviceParameterNames.isEmpty() && !originalRequestUrl.getQueryParams().isEmpty()) {
            originalRequestUrl.getQueryParams().forEach(pair -> {
                String name = pair.getName();
                if (!name.equals(protocol.getArtifactParameterName()) && !serviceParameterNames.contains(name)) {
                    if (name.contains("&") || name.contains("=")) {
                        URIBuilder encodedParamBuilder = new URIBuilder();
                        encodedParamBuilder.setParameters(name);
                        encodedParamBuilder.getQueryParams().forEach(pair2 -> {
                            String name2 = pair2.getName();
                            if (!name2.equals(protocol.getArtifactParameterName())
                                    && !serviceParameterNames.contains(name2)) {

                                builder.addParameter(name2, pair2.getValue());
                            }
                        });
                    } else {
                        builder.addParameter(name, pair.getValue());
                    }
                }
            });
        }

        String result = builder.toString();
        LOG.debug("serviceUrl generated: {}", result);
        return result;
    }

    private CASUtils() {
        // private constructor for static utility class
    }
}
