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

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.sra.RouteProvider;
import org.apache.syncope.sra.SRAProperties;
import org.apache.syncope.sra.security.web.server.DoNothingIfCommittedServerRedirectStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

public abstract class AbstractServerLogoutSuccessHandler
        implements ServerLogoutSuccessHandler, ApplicationListener<RefreshRoutesEvent> {

    private static final Map<String, Optional<URI>> CACHE = new ConcurrentHashMap<>();

    protected final ServerRedirectStrategy redirectStrategy = new DoNothingIfCommittedServerRedirectStrategy();

    @Autowired
    private RouteProvider routeProvider;

    @Autowired
    private SRAProperties props;

    @Override
    public void onApplicationEvent(final RefreshRoutesEvent event) {
        CACHE.clear();
    }

    protected URI getPostLogout(final WebFilterExchange exchange) {
        URI postLogout = props.getGlobal().getPostLogout();
        String routeId = exchange.getExchange().getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR);
        if (StringUtils.isNotBlank(routeId)) {
            Optional<URI> routePostLogout = Optional.ofNullable(CACHE.get(routeId)).orElseGet(() -> {
                Optional<SRARouteTO> route = routeProvider.getRouteTOs().stream().
                        filter(r -> routeId.equals(r.getKey())).findFirst();
                URI uri = route.map(SRARouteTO::getPostLogout).orElse(null);

                CACHE.put(routeId, Optional.ofNullable(uri));
                return CACHE.get(routeId);
            });
            if (routePostLogout.isPresent()) {
                postLogout = routePostLogout.get();
            }
        }
        return postLogout;
    }
}
