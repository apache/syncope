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
package org.apache.syncope.sra.security.saml2;

import java.util.Optional;
import org.apache.syncope.sra.SessionConfig;
import org.apache.syncope.sra.security.pac4j.NoOpSessionStore;
import org.apache.syncope.sra.security.pac4j.RedirectionActionUtils;
import org.apache.syncope.sra.security.pac4j.ServerWebExchangeContext;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.context.SAML2MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class SAML2LogoutResponseWebFilter implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2LogoutResponseWebFilter.class);

    public static final ServerWebExchangeMatcher MATCHER =
            ServerWebExchangeMatchers.pathMatchers("/logout/saml2/sso");

    private static class ServerWebExchangeLogoutContext extends ServerWebExchangeContext {

        ServerWebExchangeLogoutContext(final ServerWebExchange exchange) {
            super(exchange);
        }

        @Override
        public Optional<String> getRequestParameter(final String name) {
            return Pac4jConstants.LOGOUT_ENDPOINT_PARAMETER.equals(name)
                    ? Optional.of("true")
                    : super.getRequestParameter(name);
        }
    }

    private final SAML2Client saml2Client;

    private final ServerLogoutSuccessHandler logoutSuccessHandler;

    private final CacheManager cacheManager;

    public SAML2LogoutResponseWebFilter(
            final SAML2Client saml2Client,
            final SAML2ServerLogoutSuccessHandler logoutSuccessHandler,
            final CacheManager cacheManager) {

        this.saml2Client = saml2Client;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.cacheManager = cacheManager;
    }

    private Mono<Void> handleLogoutResponse(
            final ServerWebExchange exchange, final WebFilterChain chain, final ServerWebExchangeContext swec) {

        try {
            SAML2MessageContext ctx = saml2Client.getContextProvider().
                buildContext(this.saml2Client, swec, NoOpSessionStore.INSTANCE);
            saml2Client.getLogoutProfileHandler().receive(ctx);
        } catch (OkAction e) {
            LOG.debug("LogoutResponse was actually validated but no postLogoutURL was set", e);
        } catch (Exception e) {
            LOG.error("Could not validate LogoutResponse", e);
        }

        return logoutSuccessHandler.onLogoutSuccess(new WebFilterExchange(exchange, chain), null);
    }

    private Mono<Void> handleLogoutRequest(
            final ServerWebExchange exchange, final WebFilterChain chain, final ServerWebExchangeContext swec) {

        return exchange.getSession().
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(session -> {
                    cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evictIfPresent(session.getId());

                    return session.invalidate().then(Mono.defer(() -> {
                        try {
                            saml2Client.getCredentialsExtractor().extract(swec, NoOpSessionStore.INSTANCE);
                        } catch (RedirectionAction action) {
                            return RedirectionActionUtils.handle(action, swec);
                        }

                        return chain.filter(exchange).then(Mono.empty());
                    }));
                });
    }

    private Mono<Void> handleGET(final ServerWebExchange exchange, final WebFilterChain chain) {
        if (exchange.getRequest().getQueryParams().getFirst("SAMLResponse") != null) {
            return handleLogoutResponse(exchange, chain, new ServerWebExchangeContext(exchange));
        } else if (exchange.getRequest().getQueryParams().getFirst("SAMLRequest") != null) {
            return handleLogoutRequest(exchange, chain, new ServerWebExchangeLogoutContext(exchange));
        }

        return chain.filter(exchange).then(Mono.empty());
    }

    private Mono<Void> handlePOST(final ServerWebExchange exchange, final WebFilterChain chain) {
        return exchange.getFormData().flatMap(form -> {
            if (form.containsKey("SAMLResponse")) {
                return handleLogoutResponse(exchange, chain, new ServerWebExchangeContext(exchange).setForm(form));
            } else if (form.containsKey("SAMLRequest")) {
                return handleLogoutRequest(exchange, chain, new ServerWebExchangeLogoutContext(exchange).setForm(form));
            }

            return chain.filter(exchange).then(Mono.empty());
        });
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return MATCHER.matches(exchange).
                filter(MatchResult::isMatch).
                switchIfEmpty(chain.filter(exchange).then(Mono.empty())).
                flatMap(matchResult -> {
                    return exchange.getRequest().getMethod() == HttpMethod.GET
                            ? handleGET(exchange, chain)
                            : exchange.getRequest().getMethod() == HttpMethod.POST
                            ? handlePOST(exchange, chain)
                            : Mono.error(() -> new UnsupportedOperationException(
                            "Unsupported HTTP method: " + exchange.getRequest().getMethod()));
                });
    }
}
