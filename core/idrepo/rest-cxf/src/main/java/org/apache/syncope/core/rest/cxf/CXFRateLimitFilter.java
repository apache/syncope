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
package org.apache.syncope.core.rest.cxf;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.commons.lang3.StringUtils;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class CXFRateLimitFilter implements ContainerRequestFilter {

    public static final String CACHE_NAME =
            "org.apache.syncope.core.rest.cxf.CXFRateLimitFilter";

    protected record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    }

    public record ClientWindow(long windowStartMillis, int count, long lockedUntilMillis) implements Serializable {

        private static final long serialVersionUID = -473897805205955157L;
    }

    public static MutableConfiguration<String, ClientWindow> cacheConfiguration(final RESTProperties.RateLimit props) {
        return new MutableConfiguration<String, ClientWindow>().
                setTypes(String.class, ClientWindow.class).
                setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(
                        new javax.cache.expiry.Duration(TimeUnit.MILLISECONDS, cacheExpiryMillis(props))));
    }

    protected final RESTProperties.RateLimit props;

    protected final Cache<String, ClientWindow> clients;

    @Context
    protected HttpServletRequest request;

    public CXFRateLimitFilter(final RESTProperties props, final Cache<String, ClientWindow> clients) {
        this.props = props.getRateLimit();
        this.clients = clients;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        if (!props.isEnabled() || props.getMaxRequests() <= 0) {
            return;
        }

        if (isExcluded()) {
            return;
        }

        String key = clientAddress();
        RateLimitDecision decision = allow(key, System.currentTimeMillis());
        if (!decision.allowed()) {
            requestContext.abortWith(Response.status(429).
                    header(HttpHeaders.RETRY_AFTER, decision.retryAfterSeconds()).
                    build());
        }
    }

    protected RateLimitDecision allow(final String key, final long now) {
        return clients.invoke(key, (entry, args) -> {
            ClientWindow client = entry.exists()
                    ? entry.getValue()
                    : new ClientWindow(now, 0, 0);

            if (now < client.lockedUntilMillis()) {
                return new RateLimitDecision(false, retryAfterSeconds(client.lockedUntilMillis() - now));
            }

            long windowStartMillis = client.windowStartMillis();
            int count = client.count();
            if (now - windowStartMillis >= toMillis(props.getWindow())) {
                windowStartMillis = now;
                count = 0;
            }

            count++;
            if (count > props.getMaxRequests()) {
                entry.setValue(new ClientWindow(windowStartMillis, count, now + toMillis(props.getLock())));
                return new RateLimitDecision(false, retryAfterSeconds(toMillis(props.getLock())));
            }

            entry.setValue(new ClientWindow(windowStartMillis, count, client.lockedUntilMillis()));
            return new RateLimitDecision(true, 0);
        });
    }

    protected String clientAddress() {
        String remoteAddress = remoteAddress();

        if (props.getTrustedProxies().contains(remoteAddress)) {
            String forwardedFor = Optional.ofNullable(request).
                    map(req -> req.getHeader(props.getForwardedForHeader())).
                    flatMap(CXFRateLimitFilter::firstForwardedFor).
                    orElse(null);
            if (StringUtils.isNotBlank(forwardedFor)) {
                return forwardedFor;
            }
        }

        return remoteAddress;
    }

    protected boolean isExcluded() {
        return props.getExcludedAddresses().contains(remoteAddress());
    }

    protected String remoteAddress() {
        return requestRemoteAddress().
                filter(StringUtils::isNotBlank).
                orElse("unknown");
    }

    protected Optional<String> requestRemoteAddress() {
        try {
            return Optional.ofNullable(request).map(HttpServletRequest::getRemoteAddr);
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    protected static Optional<String> firstForwardedFor(final String header) {
        return Arrays.stream(StringUtils.split(header, ',')).
                map(String::trim).
                filter(StringUtils::isNotBlank).
                findFirst();
    }

    protected long toMillis(final Duration duration) {
        return Math.max(1L, Optional.ofNullable(duration).orElse(Duration.ofMinutes(1)).toMillis());
    }

    protected static long cacheExpiryMillis(final RESTProperties.RateLimit props) {
        long windowMillis = Optional.ofNullable(props.getWindow()).orElse(Duration.ofMinutes(1)).toMillis();
        long lockMillis = Optional.ofNullable(props.getLock()).orElse(Duration.ofMinutes(1)).toMillis();
        return Math.max(1L, Math.max(windowMillis, lockMillis));
    }

    protected static long retryAfterSeconds(final long millis) {
        return Math.max(1L, (long) Math.ceil(millis / 1000.0d));
    }
}
