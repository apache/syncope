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

public class AuthenticationThrottler extends AbstractThrottler {

    public static final String CACHE = "AuthenticationThrottlerCache";

    protected static String key(final String domain, final String username) {
        return StringUtils.defaultString(domain) + ':' + StringUtils.defaultString(username);
    }

    protected static long retryAfterSeconds(final long blockedUntil, final long now) {
        return Math.max(1, TimeUnit.MILLISECONDS.toSeconds(blockedUntil - now));
    }

    public AuthenticationThrottler(
            final SecurityProperties securityProperties,
            final Cache<String, ThrottlerAttempts> attempts) {

        super(securityProperties.getAuthenticationThrottle(), attempts);
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

            ThrottlerAttempts state = entry.getValue();
            if (state.blockedUntil() > now) {
                return retryAfterSeconds(state.blockedUntil(), now);
            }

            Deque<Long> failures = prune(state.failures(), now);
            if (failures.isEmpty()) {
                entry.remove();
            } else {
                entry.setValue(new ThrottlerAttempts(failures, state.blockedUntil()));
            }
            return null;
        });
        if (retryAfter != null) {
            throw new AuthenticationThrottleException(retryAfter);
        }
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
            ThrottlerAttempts state = entry.exists()
                    ? entry.getValue()
                    : new ThrottlerAttempts();
            Deque<Long> failures = prune(state.failures(), now);
            failures.addLast(now);

            if (failures.size() >= throttle.getMaxAttempts()) {
                long blockedUntil = now + TimeUnit.SECONDS.toMillis(throttle.getLockSeconds());
                entry.setValue(new ThrottlerAttempts(failures, blockedUntil));
                return retryAfterSeconds(blockedUntil, now);
            }

            entry.setValue(new ThrottlerAttempts(failures, state.blockedUntil()));
            return null;
        });
        if (retryAfter != null) {
            throw new AuthenticationThrottleException(retryAfter);
        }
    }
}
