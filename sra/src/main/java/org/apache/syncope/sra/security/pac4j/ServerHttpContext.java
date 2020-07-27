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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.util.CommonHelper;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;

public class ServerHttpContext implements WebContext {

    private final ServerWebExchange exchange;

    private ServerHttpSessionStore sessionStore;

    private MultiValueMap<String, String> form;

    private String body;

    /**
     * Build a WebFlux context from the current exchange and web session.
     *
     * @param exchange the current exchange
     * @param webSession the current web session
     */
    public ServerHttpContext(final ServerWebExchange exchange, final WebSession webSession) {
        this(exchange, new ServerHttpSessionStore(webSession));
    }

    /**
     * Build a WebFlux context from the current exhange and from a session store.
     *
     * @param exchange the current exchange
     * @param sessionStore the session store to use
     */
    public ServerHttpContext(
            final ServerWebExchange exchange,
            final ServerHttpSessionStore sessionStore) {

        CommonHelper.assertNotNull("exchange", exchange);
        CommonHelper.assertNotNull("sessionStore", sessionStore);
        this.exchange = exchange;
        this.sessionStore = sessionStore;
    }

    public ServerHttpSessionStore getNativeSessionStore() {
        return this.sessionStore;
    }

    @Override
    public SessionStore<ServerHttpContext> getSessionStore() {
        return this.sessionStore;
    }

    @Override
    public Optional<String> getRequestAttribute(final String name) {
        return Optional.ofNullable(exchange.getAttribute(name));
    }

    @Override
    public void setRequestAttribute(final String name, final Object value) {
        exchange.getAttributes().put(name, value);
    }

    @Override
    public Optional<String> getRequestParameter(final String name) {
        Map<String, String[]> params = getRequestParameters();
        if (params.containsKey(name)) {
            String[] values = params.get(name);
            if (!ArrayUtils.isEmpty(values)) {
                return Optional.of(values[0]);
            }
        }
        return Optional.empty();
    }

    public ServerHttpContext setForm(final MultiValueMap<String, String> form) {
        this.form = form;
        return this;
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        Map<String, String[]> params = new HashMap<>();

        this.exchange.getRequest().getQueryParams().
                forEach((key, value) -> params.put(key, new String[] { value.toString() }));

        if (this.form != null) {
            form.forEach((key, values) -> params.put(key, values.toArray(new String[0])));
        }

        return params;
    }

    @Override
    public Optional<String> getRequestHeader(final String name) {
        return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(name));
    }

    @Override
    public String getRequestMethod() {
        return this.exchange.getRequest().getMethodValue();
    }

    @Override
    public String getRemoteAddr() {
        return this.exchange.getRequest().getRemoteAddress().getHostString();
    }

    /**
     * Return the native exchange.
     *
     * @return the native exchange
     */
    public ServerWebExchange getNative() {
        return this.exchange;
    }

    @Override
    public void setResponseHeader(final String name, final String value) {
        this.exchange.getResponse().getHeaders().set(name, value);
    }

    @Override
    public void setResponseContentType(final String content) {
        this.exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, content);
    }

    @Override
    public String getProtocol() {
        return isSecure() ? "https" : "http";
    }

    @Override
    public String getServerName() {
        return UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build().getHost();
    }

    @Override
    public int getServerPort() {
        return UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build().getPort();
    }

    @Override
    public String getScheme() {
        return UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build().getScheme();
    }

    @Override
    public boolean isSecure() {
        return this.exchange.getRequest().getSslInfo() != null;
    }

    @Override
    public String getFullRequestURL() {
        return UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build().toUriString();
    }

    @Override
    public Collection<Cookie> getRequestCookies() {
        MultiValueMap<String, HttpCookie> cookies = this.exchange.getRequest().getCookies();

        Collection<Cookie> pac4jCookies = new LinkedHashSet<>();
        cookies.toSingleValueMap().values().forEach(c -> {
            Cookie cookie = new Cookie(c.getName(), c.getValue());
            pac4jCookies.add(cookie);
        });
        return pac4jCookies;
    }

    @Override
    public void addResponseCookie(final Cookie cookie) {
        ResponseCookie.ResponseCookieBuilder c = ResponseCookie.from(cookie.getName(), cookie.getValue());
        c.secure(cookie.isSecure());
        c.path(cookie.getPath());
        c.maxAge(cookie.getMaxAge());
        c.httpOnly(cookie.isHttpOnly());
        c.domain(cookie.getDomain());
        this.exchange.getResponse().addCookie(c.build());
    }

    @Override
    public String getPath() {
        return exchange.getRequest().getPath().value();
    }

    public ServerHttpContext setBody(final String body) {
        this.body = body;
        return this;
    }

    @Override
    public String getRequestContent() {
        return body;
    }
}
