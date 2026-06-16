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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import javax.cache.Cache;
import org.apache.syncope.core.spring.security.SecurityProperties;

abstract class AbstractThrottler {

    protected final SecurityProperties.ThrottleProperties throttle;

    protected final LongSupplier clock = System::currentTimeMillis;

    protected final Cache<String, ThrottlerAttempts> attempts;

    protected AbstractThrottler(
            final SecurityProperties.ThrottleProperties throttle,
            final Cache<String, ThrottlerAttempts> attempts) {

        this.throttle = throttle;
        this.attempts = attempts;
    }

    protected boolean isEnabled() {
        return throttle.isEnabled()
                && throttle.getMaxAttempts() > 0
                && throttle.getWindowSeconds() > 0
                && throttle.getLockSeconds() > 0;
    }

    protected Deque<Long> prune(final Deque<Long> attempts, final long now) {
        Deque<Long> failures = new ArrayDeque<>(attempts);
        long threshold = now - TimeUnit.SECONDS.toMillis(throttle.getWindowSeconds());
        while (!failures.isEmpty() && failures.peekFirst() < threshold) {
            failures.removeFirst();
        }
        return failures;
    }
}
