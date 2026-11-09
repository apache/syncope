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
package org.apache.syncope.core.starter.actuate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class DomainsHealthIndicatorTest {

    private static final class TestDomainHolder implements DomainHolder<Object> {

        private final Map<String, Boolean> healthInfo;

        TestDomainHolder(final Map<String, Boolean> healthInfo) {
            this.healthInfo = healthInfo;
        }

        @Override
        public Map<String, Object> getDomains() {
            Map<String, Object> domains = new HashMap<>();
            healthInfo.keySet().forEach(domain -> domains.put(domain, domain));
            return domains;
        }

        @Override
        public Map<String, Boolean> getHealthInfo() {
            return healthInfo;
        }
    }

    private Health health(final Map<String, Boolean> healthInfo) {
        return new DomainsHealthIndicator(new TestDomainHolder(healthInfo)).health();
    }

    @Test
    void isUpWhenAllDomainsAreUp() {
        Health result = health(Map.of("Master", true, "Other", true));

        assertEquals(Status.UP, result.getStatus());
        assertEquals(Map.of("Master", Status.UP, "Other", Status.UP), result.getDetails());
    }

    @Test
    void isDownWhenAnyDomainIsDown() {
        Health result = health(Map.of("Master", true, "Other", false));

        assertEquals(Status.DOWN, result.getStatus());
        assertEquals(Map.of("Master", Status.UP, "Other", Status.DOWN), result.getDetails());
    }

    @Test
    void isUpWhenNoDomainIsConfigured() {
        assertEquals(Status.UP, health(Map.of()).getStatus());
    }
}
