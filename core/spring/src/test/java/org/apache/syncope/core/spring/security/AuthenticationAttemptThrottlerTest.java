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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.cache.Cache;
import javax.cache.Caching;
import org.junit.jupiter.api.Test;

public class AuthenticationAttemptThrottlerTest {

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

        Cache<String, AuthenticationAttemptThrottler.Attempts> authenticationAttemptCache =
                Caching.getCachingProvider().getCacheManager().createCache(
                        AuthenticationAttemptThrottlerTest.class.getName() + '-' + UUID.randomUUID(),
                        AuthenticationAttemptThrottler.cacheConfiguration(
                                securityProperties.getAuthenticationThrottle()));

        return new AuthenticationAttemptThrottler(securityProperties, clock, authenticationAttemptCache);
    }

    @Test
    public void blocksAfterConfiguredFailures() {
        AtomicLong now = new AtomicLong();
        SecurityProperties securityProperties = securityProperties();
        AuthenticationAttemptThrottler throttler =
                throttler(securityProperties, now::get);

        for (int i = 1; i < securityProperties.getAuthenticationThrottle().getMaxAttempts(); i++) {
            assertDoesNotThrow(() -> throttler.recordFailure("Master", "rossini"));
        }
        assertThrows(RateLimitAuthenticationException.class,
                () -> throttler.recordFailure("Master", "rossini"));
        assertThrows(RateLimitAuthenticationException.class,
                () -> throttler.checkAllowed("Master", "rossini"));

        now.addAndGet(30_000);
        assertDoesNotThrow(() -> throttler.checkAllowed("Master", "rossini"));
    }

    @Test
    public void successResetsFailures() {
        AtomicLong now = new AtomicLong();
        AuthenticationAttemptThrottler throttler =
                throttler(securityProperties(), now::get);

        throttler.recordFailure("Master", "rossini");
        throttler.clearFailures("Master", "rossini");

        assertDoesNotThrow(() -> throttler.recordFailure("Master", "rossini"));
    }

    @Test
    public void expiredFailuresAreIgnored() {
        AtomicLong now = new AtomicLong();
        AuthenticationAttemptThrottler throttler =
                throttler(securityProperties(), now::get);

        throttler.recordFailure("Master", "rossini");
        now.addAndGet(61_000);

        assertDoesNotThrow(() -> throttler.recordFailure("Master", "rossini"));
    }
}
