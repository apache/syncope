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
package org.apache.syncope.core.spring.security.throttle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.Test;

class PasswordResetRequestThrottlerTest {

    private static final String DOMAIN = SyncopeConstants.MASTER_DOMAIN;

    private static final String USERNAME = "rossini";

    private static final String CLIENT = "127.0.0.1";

    private static SecurityProperties securityProperties(final boolean enabled) {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getPasswordResetThrottle().setEnabled(enabled);
        securityProperties.getPasswordResetThrottle().setMaxAttempts(3);
        securityProperties.getPasswordResetThrottle().setWindowSeconds(60);
        securityProperties.getPasswordResetThrottle().setLockSeconds(30);
        return securityProperties;
    }

    private static PasswordResetRequestThrottler throttler(
            final SecurityProperties securityProperties,
            final LongSupplier clock) {

        Cache<String, ThrottlerAttempts> cache =
                Caching.getCachingProvider().getCacheManager().getCache(PasswordResetRequestThrottler.CACHE);
        if (cache == null) {
            cache = Caching.getCachingProvider().getCacheManager().createCache(
                    PasswordResetRequestThrottler.CACHE,
                    new MutableConfiguration<String, ThrottlerAttempts>().
                            setTypes(String.class, ThrottlerAttempts.class).
                            setExpiryPolicyFactory(
                                    TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 30))));
        } else {
            cache.clear();
        }

        return new PasswordResetRequestThrottler(securityProperties, cache) {

            @Override
            protected long now() {
                return clock.getAsLong();
            }
        };
    }

    @Test
    void disabledThrottlerNeverBlocks() {
        PasswordResetRequestThrottler throttler = throttler(securityProperties(false), () -> 0L);

        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        }
    }

    @Test
    void blocksAfterConfiguredAttempts() {
        AtomicLong now = new AtomicLong(1_000_000L);
        PasswordResetRequestThrottler throttler = throttler(securityProperties(true), now::get);

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        }

        PasswordResetThrottleException ex = assertThrows(
                PasswordResetThrottleException.class,
                () -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        assertEquals(30, ex.getRetryAfterSeconds());

        assertThrows(
                PasswordResetThrottleException.class,
                () -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
    }

    @Test
    void blockedClientUnblocksAfterLockPeriod() {
        AtomicLong now = new AtomicLong(1_000_000L);
        PasswordResetRequestThrottler throttler = throttler(securityProperties(true), now::get);

        for (int i = 0; i < 4; i++) {
            try {
                throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT);
            } catch (PasswordResetThrottleException e) {
                // expected on the fourth attempt
            }
        }

        now.addAndGet(TimeUnit.SECONDS.toMillis(61));
        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
    }

    @Test
    void failuresOutsideWindowArePruned() {
        AtomicLong now = new AtomicLong(1_000_000L);
        PasswordResetRequestThrottler throttler = throttler(securityProperties(true), now::get);

        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        now.addAndGet(TimeUnit.SECONDS.toMillis(61));

        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
    }

    @Test
    void keysAreIndependent() {
        PasswordResetRequestThrottler throttler = throttler(securityProperties(true), () -> 1_000_000L);

        for (int i = 0; i < 4; i++) {
            try {
                throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT);
            } catch (PasswordResetThrottleException e) {
                // expected on the fourth attempt
            }
        }

        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, "other", CLIENT));
        assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, "10.0.0.1"));
        assertDoesNotThrow(() -> throttler.recordAndCheck("other", USERNAME, CLIENT));
    }

    @Test
    void keyHandlesNullValues() {
        assertEquals("::", PasswordResetRequestThrottler.key(null, null, null));
        assertEquals(
                PasswordResetRequestThrottler.key(DOMAIN, USERNAME, CLIENT),
                PasswordResetRequestThrottler.key(DOMAIN, USERNAME, CLIENT));
    }

    @Test
    void misconfiguredThrottlerIsDisabled() {
        SecurityProperties securityProperties = securityProperties(true);
        securityProperties.getPasswordResetThrottle().setMaxAttempts(0);
        PasswordResetRequestThrottler throttler = throttler(securityProperties, () -> 1_000_000L);

        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> throttler.recordAndCheck(DOMAIN, USERNAME, CLIENT));
        }
    }
}
