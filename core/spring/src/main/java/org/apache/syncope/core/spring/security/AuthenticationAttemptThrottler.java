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

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.commons.lang3.StringUtils;

public class AuthenticationAttemptThrottler {

    public static final String CACHE_NAME =
            "org.apache.syncope.core.spring.security.AuthenticationAttemptThrottler";

    public record Attempts(Deque<Long> failures, long blockedUntil) implements Serializable {

        private static final long serialVersionUID = 8023582605543650484L;

        public Attempts {
            failures = new ArrayDeque<>(failures);
        }

        private Attempts() {
            this(new ArrayDeque<>(), 0);
        }
    }

    protected final SecurityProperties.AuthenticationThrottleProperties throttle;

    protected final LongSupplier clock;

    protected final Cache<String, Attempts> attempts;

    public AuthenticationAttemptThrottler(
            final SecurityProperties securityProperties,
            final Cache<String, Attempts> attempts) {

        this(securityProperties, System::currentTimeMillis, attempts);
    }

    protected AuthenticationAttemptThrottler(
            final SecurityProperties securityProperties,
            final LongSupplier clock,
            final Cache<String, Attempts> attempts) {

        this.throttle = securityProperties.getAuthenticationThrottle();
        this.clock = clock;
        this.attempts = attempts;
    }

    public static MutableConfiguration<String, Attempts> cacheConfiguration(
            final SecurityProperties.AuthenticationThrottleProperties throttle) {

        return new MutableConfiguration<String, Attempts>().
                setTypes(String.class, Attempts.class).
                setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(
                        new javax.cache.expiry.Duration(TimeUnit.SECONDS, cacheExpirySeconds(throttle))));
    }

    protected static long cacheExpirySeconds(final SecurityProperties.AuthenticationThrottleProperties throttle) {
        return Math.max(1, Math.max(throttle.getWindowSeconds(), throttle.getLockSeconds()));
    }

    protected static String key(final String domain, final String username) {
        return StringUtils.defaultString(domain) + ':' + StringUtils.defaultString(username);
    }

    public void checkAllowed(final String domain, final String username) {
        if (!isEnabled()) {
            return;
        }

        String key = key(domain, username);
        long now = clock.getAsLong();
        Long retryAfter = attempts.invoke(key, (entry, args) -> {
            if (!entry.exists()) {
                return null;
            }

            Attempts state = entry.getValue();
            if (state.blockedUntil() > now) {
                return retryAfterSeconds(state.blockedUntil(), now);
            }

            Deque<Long> failures = prune(state.failures(), now);
            if (failures.isEmpty()) {
                entry.remove();
            } else {
                entry.setValue(new Attempts(failures, state.blockedUntil()));
            }
            return null;
        });
        if (retryAfter != null) {
            throw blocked(retryAfter);
        }
    }

    protected boolean isEnabled() {
        return throttle.isEnabled()
                && throttle.getMaxAttempts() > 0
                && throttle.getWindowSeconds() > 0
                && throttle.getLockSeconds() > 0;
    }

    public void clearFailures(final String domain, final String username) {
        attempts.remove(key(domain, username));
    }

    public void recordFailure(final String domain, final String username) {
        if (!isEnabled()) {
            return;
        }

        long now = clock.getAsLong();
        Long retryAfter = attempts.invoke(key(domain, username), (entry, args) -> {
            Attempts state = entry.exists()
                    ? entry.getValue()
                    : new Attempts();
            Deque<Long> failures = prune(state.failures(), now);
            failures.addLast(now);

            if (failures.size() >= throttle.getMaxAttempts()) {
                long blockedUntil = now + TimeUnit.SECONDS.toMillis(throttle.getLockSeconds());
                entry.setValue(new Attempts(failures, blockedUntil));
                return retryAfterSeconds(blockedUntil, now);
            }

            entry.setValue(new Attempts(failures, state.blockedUntil()));
            return null;
        });
        if (retryAfter != null) {
            throw blocked(retryAfter);
        }
    }

    protected Deque<Long> prune(final Deque<Long> attempts, final long now) {
        Deque<Long> failures = new ArrayDeque<>(attempts);
        long threshold = now - TimeUnit.SECONDS.toMillis(throttle.getWindowSeconds());
        while (!failures.isEmpty() && failures.peekFirst() < threshold) {
            failures.removeFirst();
        }
        return failures;
    }

    protected static long retryAfterSeconds(final long blockedUntil, final long now) {
        return Math.max(1, TimeUnit.MILLISECONDS.toSeconds(blockedUntil - now));
    }

    protected static RateLimitAuthenticationException blocked(final long retryAfter) {
        return new RateLimitAuthenticationException("Too many authentication failures", retryAfter);
    }
}
