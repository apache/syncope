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

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.core.provisioning.java.ExecutorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("rest")
public class RESTProperties {

    public static class RateLimitProperties {

        private boolean enabled;

        private int maxRequests = 300;

        private Duration window = Duration.ofMinutes(1);

        private Duration lock = Duration.ofMinutes(1);

        private String forwardedForHeader = "X-Forwarded-For";

        private final Set<String> excludedAddresses = new HashSet<>();

        private final Set<String> trustedProxies = new HashSet<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(final int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(final Duration window) {
            this.window = window;
        }

        public Duration getLock() {
            return lock;
        }

        public void setLock(final Duration lock) {
            this.lock = lock;
        }

        public String getForwardedForHeader() {
            return forwardedForHeader;
        }

        public void setForwardedForHeader(final String forwardedForHeader) {
            this.forwardedForHeader = forwardedForHeader;
        }

        public Set<String> getExcludedAddresses() {
            return excludedAddresses;
        }

        public Set<String> getTrustedProxies() {
            return trustedProxies;
        }
    }

    @NestedConfigurationProperty
    private final ExecutorProperties batchExecutor = new ExecutorProperties();

    @NestedConfigurationProperty
    private final RateLimitProperties rateLimitProperties = new RateLimitProperties();

    public ExecutorProperties getBatchExecutor() {
        return batchExecutor;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimitProperties;
    }
}
