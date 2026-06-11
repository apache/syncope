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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class CXFRateLimitPropertiesFilterTest {

    private static Cache<String, CXFRateLimitFilter.ClientWindow> cache(final RESTProperties props) {
        return Caching.getCachingProvider().getCacheManager().createCache(
                CXFRateLimitPropertiesFilterTest.class.getName() + '-' + UUID.randomUUID(),
                cacheConfiguration(props.getRateLimit()));
    }

    private static MutableConfiguration<String, CXFRateLimitFilter.ClientWindow> cacheConfiguration(
            final RESTProperties.RateLimitProperties props) {

        return new MutableConfiguration<String, CXFRateLimitFilter.ClientWindow>().
                setTypes(String.class, CXFRateLimitFilter.ClientWindow.class).
                setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(
                        new javax.cache.expiry.Duration(TimeUnit.MILLISECONDS, cacheExpiryMillis(props))));
    }

    private static long cacheExpiryMillis(final RESTProperties.RateLimitProperties props) {
        long windowMillis = Optional.ofNullable(props.getWindow()).orElse(Duration.ofMinutes(1)).toMillis();
        long lockMillis = Optional.ofNullable(props.getLock()).orElse(Duration.ofMinutes(1)).toMillis();
        return Math.max(1L, Math.max(windowMillis, lockMillis));
    }

    private static CXFRateLimitFilter filter(final MockHttpServletRequest request) {
        RESTProperties props = new RESTProperties();
        props.getRateLimit().setEnabled(true);
        props.getRateLimit().setMaxRequests(2);
        props.getRateLimit().setWindow(Duration.ofMinutes(1));
        props.getRateLimit().setLock(Duration.ofSeconds(30));

        CXFRateLimitFilter filter = new CXFRateLimitFilter(props, cache(props));
        ReflectionTestUtils.setField(filter, "request", request);
        return filter;
    }

    @Test
    void blocksWhenClientExceedsLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");

        CXFRateLimitFilter filter = filter(request);
        ContainerRequestContext requestContext = org.mockito.Mockito.mock(ContainerRequestContext.class);

        filter.filter(requestContext);
        filter.filter(requestContext);
        verify(requestContext, never()).abortWith(org.mockito.Mockito.any());

        filter.filter(requestContext);

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(response.capture());
        assertEquals(429, response.getValue().getStatus());
        assertEquals("30", response.getValue().getHeaderString(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void usesForwardedForOnlyFromTrustedProxy() {
        RESTProperties props = new RESTProperties();
        props.getRateLimit().setEnabled(true);
        props.getRateLimit().setMaxRequests(1);
        props.getRateLimit().getTrustedProxies().add("127.0.0.1");

        MockHttpServletRequest trustedProxyRequest = new MockHttpServletRequest();
        trustedProxyRequest.setRemoteAddr("127.0.0.1");
        trustedProxyRequest.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");

        CXFRateLimitFilter trustedProxyFilter = new CXFRateLimitFilter(props, cache(props));
        ReflectionTestUtils.setField(trustedProxyFilter, "request", trustedProxyRequest);
        assertEquals("203.0.113.10", trustedProxyFilter.clientAddress());

        MockHttpServletRequest untrustedRequest = new MockHttpServletRequest();
        untrustedRequest.setRemoteAddr("198.51.100.30");
        untrustedRequest.addHeader("X-Forwarded-For", "203.0.113.10");

        CXFRateLimitFilter untrustedFilter = new CXFRateLimitFilter(props, cache(props));
        ReflectionTestUtils.setField(untrustedFilter, "request", untrustedRequest);
        assertEquals("198.51.100.30", untrustedFilter.clientAddress());
    }

    @Test
    void skipsExcludedRemoteAddress() {
        RESTProperties props = new RESTProperties();
        props.getRateLimit().setEnabled(true);
        props.getRateLimit().setMaxRequests(1);
        props.getRateLimit().getExcludedAddresses().add("10.0.0.20");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.20");

        CXFRateLimitFilter filter = new CXFRateLimitFilter(props, cache(props));
        ReflectionTestUtils.setField(filter, "request", request);
        ContainerRequestContext requestContext = org.mockito.Mockito.mock(ContainerRequestContext.class);

        filter.filter(requestContext);
        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(org.mockito.Mockito.any());
    }
}
