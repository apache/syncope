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

import java.util.Deque;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordResetRequestThrottler extends AbstractThrottler {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordResetRequestThrottler.class);

    public static final String CACHE = "PasswordResetRequestThrottlerCache";

    protected static String key(final String domain, final String username, final String clientAddress) {
        return StringUtils.defaultString(domain)
                + ':' + StringUtils.defaultString(username)
                + ':' + StringUtils.defaultString(clientAddress);
    }

    protected static String attemptKeyId(final String key) {
        return Integer.toUnsignedString(key.hashCode(), 16);
    }

    protected static PasswordResetThrottleException blocked(final long blockedUntil, final long now) {
        long retryAfter = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(blockedUntil - now));
        return new PasswordResetThrottleException(retryAfter);
    }

    public PasswordResetRequestThrottler(
            final SecurityProperties securityProperties,
            final Cache<String, ThrottlerAttempts> attempts) {

        super(securityProperties.getPasswordResetThrottle(), attempts);
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
                    ThrottlerAttempts state = entry.exists()
                    ? entry.getValue()
                    : new ThrottlerAttempts();

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
                        entry.setValue(new ThrottlerAttempts(failures, blockedUntil));
                        LOG.warn(
                                "Password reset request throttling activated for attempt key [{}]; "
                                + "attempts [{}], max attempts [{}], lock seconds [{}]",
                                attemptKeyId,
                                failures.size(),
                                throttle.getMaxAttempts(),
                                throttle.getLockSeconds());
                        return blocked(blockedUntil, now);
                    }

                    entry.setValue(new ThrottlerAttempts(failures, state.blockedUntil()));
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
}
