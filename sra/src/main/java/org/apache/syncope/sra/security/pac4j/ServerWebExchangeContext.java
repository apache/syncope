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
import org.pac4j.core.util.CommonHelper;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.ForwardedHeaderUtils;

public class ServerWebExchangeContext implements WebContext {

    private final ServerWebExchange exchange;

    private MultiValueMap<String, String> form;

    private String body;

    /**
     * Build a WebFlux context from the current exchange.
     *
     * @param exchange the current exchange
     */
    public ServerWebExchangeContext(final ServerWebExchange exchange) {
        CommonHelper.assertNotNull("exchange", exchange);
        this.exchange = exchange;
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

    public ServerWebExchangeContext setForm(final MultiValueMap<String, String> form) {
        this.form = form;
        return this;
    }

    @Override
    public Optional<String> getQueryString() {
        return Optional.ofNullable(exchange.getRequest().getURI().getQuery());
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        Map<String, String[]> params = new HashMap<>();

        exchange.getRequest().getQueryParams().
                forEach((key, value) -> params.put(key, new String[] { value.toString() }));

        if (form != null) {
            form.forEach((key, values) -> params.put(key, values.toArray(String[]::new)));
        }

        return params;
    }

    @Override
    public Optional<String> getRequestHeader(final String name) {
        return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(name));
    }

    @Override
    public String getRequestMethod() {
        return exchange.getRequest().getMethod().name();
    }

    @Override
    public String getRemoteAddr() {
        return exchange.getRequest().getRemoteAddress().getHostString();
    }

    /**
     * Return the native exchange.
     *
     * @return the native exchange
     */
    public ServerWebExchange getNative() {
        return exchange;
    }

    @Override
    public void setResponseHeader(final String name, final String value) {

    }

    @Override
    public Optional<String> getResponseHeader(final String s) {
        return Optional.ofNullable(exchange.getResponse().getHeaders().getFirst(s));
    }

    @Override
    public void setResponseContentType(final String content) {
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, content);
    }

    @Override
    public String getProtocol() {
        return isSecure() ? "https" : "http";
    }

    @Override
    public String getServerName() {
        return ForwardedHeaderUtils.adaptFromForwardedHeaders(
                exchange.getRequest().getURI(), exchange.getRequest().getHeaders()).build().getHost();
    }

    @Override
    public int getServerPort() {
        return ForwardedHeaderUtils.adaptFromForwardedHeaders(
                exchange.getRequest().getURI(), exchange.getRequest().getHeaders()).build().getPort();
    }

    @Override
    public String getScheme() {
        return ForwardedHeaderUtils.adaptFromForwardedHeaders(
                exchange.getRequest().getURI(), exchange.getRequest().getHeaders()).build().getScheme();
    }

    @Override
    public boolean isSecure() {
        return exchange.getRequest().getSslInfo() != null;
    }

    @Override
    public String getFullRequestURL() {
        return ForwardedHeaderUtils.adaptFromForwardedHeaders(
                exchange.getRequest().getURI(), exchange.getRequest().getHeaders()).build().toUriString();
    }

    @Override
    public Collection<Cookie> getRequestCookies() {
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

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
        exchange.getResponse().addCookie(c.build());
    }

    @Override
    public String getPath() {
        return exchange.getRequest().getPath().value();
    }

    public ServerWebExchangeContext setBody(final String body) {
        this.body = body;
        return this;
    }

    @Override
    public String getRequestContent() {
        return body;
    }
}
