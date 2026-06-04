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
package org.apache.syncope.core.logic;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordResetRequestThrottler {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordResetRequestThrottler.class);

    public static final String CACHE_NAME =
            "org.apache.syncope.core.logic.PasswordResetRequestThrottler";

    public record Attempts(Deque<Long> failures, long blockedUntil) implements Serializable {

        private static final long serialVersionUID = -4276590495962149303L;

        public Attempts {
            failures = new ArrayDeque<>(failures);
        }

        private Attempts() {
            this(new ArrayDeque<>(), 0);
        }
    }

    protected final SecurityProperties.PasswordResetProperties.ThrottleProperties throttle;

    protected final LongSupplier clock;

    protected final Cache<String, Attempts> attempts;

    public PasswordResetRequestThrottler(
            final SecurityProperties securityProperties,
            final Cache<String, Attempts> attempts) {

        this(securityProperties, System::currentTimeMillis, attempts);
    }

    PasswordResetRequestThrottler(
            final SecurityProperties securityProperties,
            final LongSupplier clock,
            final Cache<String, Attempts> attempts) {

        this.throttle = securityProperties.getPasswordReset().getThrottle();
        this.clock = clock;
        this.attempts = attempts;
    }

    public static MutableConfiguration<String, Attempts> cacheConfiguration(
            final SecurityProperties.PasswordResetProperties.ThrottleProperties throttle) {

        return new MutableConfiguration<String, Attempts>().
                setTypes(String.class, Attempts.class).
                setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(
                        new javax.cache.expiry.Duration(TimeUnit.SECONDS, cacheExpirySeconds(throttle))));
    }

    private static long cacheExpirySeconds(
            final SecurityProperties.PasswordResetProperties.ThrottleProperties throttle) {

        return Math.max(1, Math.max(throttle.getWindowSeconds(), throttle.getLockSeconds()));
    }

    public void recordAndCheck(final String domain, final String username, final String clientAddress) {
        if (!isEnabled()) {
            LOG.debug("Password reset request throttling skipped because it is disabled or misconfigured");
            return;
        }

        long now = clock.getAsLong();
        String attemptKey = key(domain, username, clientAddress);
        String attemptKeyId = attemptKeyId(attemptKey);
        PasswordResetThrottleException blocked = attempts.invoke(
                attemptKey,
                (entry, args) -> {
                    Attempts state = entry.exists()
                            ? entry.getValue()
                            : new Attempts();

                    if (state.blockedUntil() > now) {
                        PasswordResetThrottleException exception = blocked(state.blockedUntil(), now);
                        LOG.debug(
                                "Password reset request throttled for attempt key [{}]; retry after [{}] seconds",
                                attemptKeyId,
                                exception.getRetryAfterSeconds());
                        return exception;
                    }

                    Deque<Long> failures = prune(state.failures(), now);
                    failures.addLast(now);
                    if (failures.size() > throttle.getMaxAttempts()) {
                        long blockedUntil = now + TimeUnit.SECONDS.toMillis(throttle.getLockSeconds());
                        entry.setValue(new Attempts(failures, blockedUntil));
                        LOG.warn(
                                "Password reset request throttling activated for attempt key [{}]; "
                                + "attempts [{}], max attempts [{}], lock seconds [{}]",
                                attemptKeyId,
                                failures.size(),
                                throttle.getMaxAttempts(),
                                throttle.getLockSeconds());
                        return blocked(blockedUntil, now);
                    }

                    entry.setValue(new Attempts(failures, state.blockedUntil()));
                    LOG.trace(
                            "Password reset request failure recorded for attempt key [{}]; attempts [{}/{}]",
                            attemptKeyId,
                            failures.size(),
                            throttle.getMaxAttempts());
                    return null;
                });
        if (blocked != null) {
            throw blocked;
        }
    }

    private static String key(final String domain, final String username, final String clientAddress) {
        return StringUtils.defaultString(domain)
                + ':' + StringUtils.defaultString(username)
                + ':' + StringUtils.defaultString(clientAddress);
    }

    private static String attemptKeyId(final String key) {
        return Integer.toUnsignedString(key.hashCode(), 16);
    }

    private boolean isEnabled() {
        return throttle.isEnabled()
                && throttle.getMaxAttempts() > 0
                && throttle.getWindowSeconds() > 0
                && throttle.getLockSeconds() > 0;
    }

    private Deque<Long> prune(final Deque<Long> attempts, final long now) {
        Deque<Long> failures = new ArrayDeque<>(attempts);
        long threshold = now - TimeUnit.SECONDS.toMillis(throttle.getWindowSeconds());
        while (!failures.isEmpty() && failures.peekFirst() < threshold) {
            failures.removeFirst();
        }
        return failures;
    }

    private static PasswordResetThrottleException blocked(final long blockedUntil, final long now) {
        long retryAfter = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(blockedUntil - now));
        return new PasswordResetThrottleException(retryAfter);
    }
}
