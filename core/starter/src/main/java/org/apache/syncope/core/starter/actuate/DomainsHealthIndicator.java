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

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class DomainsHealthIndicator implements HealthIndicator {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainsHealthIndicator.class);

    protected final DomainHolder<?> domainHolder;

    public DomainsHealthIndicator(final DomainHolder<?> domainHolder) {
        this.domainHolder = domainHolder;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        Mutable<Boolean> anyDown = new MutableObject<>(false);
        domainHolder.getHealthInfo().forEach((domain, status) -> {
            builder.withDetail(domain, status ? Status.UP : Status.DOWN);
            if (!status) {
                anyDown.setValue(true);
            }
        });

        builder.status(anyDown.getValue() ? Status.DOWN : Status.UP);

        return builder.build();
    }
}
