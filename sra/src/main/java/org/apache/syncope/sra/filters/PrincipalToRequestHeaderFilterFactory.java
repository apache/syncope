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

import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.sra.SessionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory.NameConfig;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.session.Session;
import reactor.core.publisher.Mono;

public class PrincipalToRequestHeaderFilterFactory extends AbstractGatewayFilterFactory<NameConfig> {

    @Autowired
    private CacheManager cacheManager;

    public PrincipalToRequestHeaderFilterFactory() {
        super(NameConfig.class);
    }

    @Override
    public GatewayFilter apply(final NameConfig config) {
        return (exchange, chain) -> exchange.getSession().
                flatMap(session -> Mono.justOrEmpty(Optional.ofNullable(
                cacheManager.getCache(SessionConfig.DEFAULT_CACHE).get(session.getId(), Session.class)).
                map(cachedSession -> {
                    String principal = null;

                    SecurityContext ctx = cachedSession.getAttribute(
                            WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME);
                    if (ctx != null && ctx.getAuthentication() != null) {
                        if (ctx.getAuthentication().getPrincipal() instanceof final OidcUser oidcUser) {
                            principal = oidcUser.
                                    getIdToken().getTokenValue();
                        } else if (ctx.getAuthentication().getPrincipal() instanceof final OAuth2User oAuth2User) {
                            principal = Objects.toString(oAuth2User.
                                    getAttributes().get(StandardClaimNames.PREFERRED_USERNAME), null);
                        } else {
                            principal = ctx.getAuthentication().getName();
                        }
                    }

                    return principal;
                }))).
                transform(principal -> principal.flatMap(p -> StringUtils.isEmpty(p)
                ? chain.filter(exchange)
                : chain.filter(exchange.mutate().
                        request(exchange.getRequest().mutate().
                                headers(headers -> headers.add(config.getName(), p)).build()).
                        build()))).
                switchIfEmpty(chain.filter(exchange));
    }
}
