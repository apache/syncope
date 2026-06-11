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
package org.apache.syncope.core.spring.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthenticationAttemptThrottlerTest {

    private static SecurityProperties securityProperties() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getAuthenticationThrottle().setEnabled(true);
        securityProperties.getAuthenticationThrottle().setMaxAttempts(5);
        securityProperties.getAuthenticationThrottle().setWindowSeconds(60);
        securityProperties.getAuthenticationThrottle().setLockSeconds(30);
        return securityProperties;
    }

    private static AuthenticationAttemptThrottler throttler(
            final SecurityProperties securityProperties,
            final LongSupplier clock) {

        Cache<String, AuthenticationAttemptThrottler.Attempts> cache =
                Caching.getCachingProvider().getCacheManager().getCache(AuthenticationAttemptThrottler.CACHE);
        if (cache == null) {
            cache = Caching.getCachingProvider().getCacheManager().createCache(
                    AuthenticationAttemptThrottler.CACHE,
                    new MutableConfiguration<String, AuthenticationAttemptThrottler.Attempts>().
                            setTypes(String.class, AuthenticationAttemptThrottler.Attempts.class).
                            setExpiryPolicyFactory(
                                    TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 30))));
        } else {
            cache.clear();
        }

        AuthenticationAttemptThrottler throttler = new AuthenticationAttemptThrottler(securityProperties, cache);
        ReflectionTestUtils.setField(throttler, "clock", clock);
        return throttler;
    }

    @Test
    void blocksAfterConfiguredFailures() {
        AtomicLong now = new AtomicLong();
        SecurityProperties securityProperties = securityProperties();
        AuthenticationAttemptThrottler throttler = throttler(securityProperties, now::get);

        for (int i = 1; i < securityProperties.getAuthenticationThrottle().getMaxAttempts(); i++) {
            assertDoesNotThrow(() -> throttler.recordFailure(SyncopeConstants.MASTER_DOMAIN, "rossini"));
        }
        assertThrows(RateLimitAuthenticationException.class,
                () -> throttler.recordFailure(SyncopeConstants.MASTER_DOMAIN, "rossini"));
        assertThrows(RateLimitAuthenticationException.class,
                () -> throttler.checkAllowed(SyncopeConstants.MASTER_DOMAIN, "rossini"));

        now.addAndGet(30_000);
        assertDoesNotThrow(() -> throttler.checkAllowed(SyncopeConstants.MASTER_DOMAIN, "rossini"));
    }

    @Test
    void successResetsFailures() {
        AtomicLong now = new AtomicLong();
        AuthenticationAttemptThrottler throttler = throttler(securityProperties(), now::get);

        throttler.recordFailure(SyncopeConstants.MASTER_DOMAIN, "rossini");
        throttler.clearFailures(SyncopeConstants.MASTER_DOMAIN, "rossini");

        assertDoesNotThrow(() -> throttler.recordFailure(SyncopeConstants.MASTER_DOMAIN, "rossini"));
    }

    @Test
    void expiredFailuresAreIgnored() {
        AtomicLong now = new AtomicLong();
        AuthenticationAttemptThrottler throttler = throttler(securityProperties(), now::get);

        throttler.recordFailure(SyncopeConstants.MASTER_DOMAIN, "rossini");
        now.addAndGet(61_000);

        assertDoesNotThrow(() -> throttler.recordFailure(SyncopeConstants.MASTER_DOMAIN, "rossini"));
    }
}
