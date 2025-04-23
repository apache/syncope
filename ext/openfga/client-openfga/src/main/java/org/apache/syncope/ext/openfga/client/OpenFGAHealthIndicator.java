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
package org.apache.syncope.ext.openfga.client;

import java.util.Map;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.ext.openfga.client.model.ListStoresResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class OpenFGAHealthIndicator implements HealthIndicator {

    protected final DomainHolder<?> domainHolder;

    protected final OpenFGAClientFactory clientFactory;

    public OpenFGAHealthIndicator(
            final DomainHolder<?> domainHolder,
            final OpenFGAClientFactory clientFactory) {

        this.domainHolder = domainHolder;
        this.clientFactory = clientFactory;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        builder.withDetail("baseUri", clientFactory.getBaseUri());

        Mutable<Boolean> anyDown = new MutableObject<>(Boolean.FALSE);

        domainHolder.getDomains().keySet().forEach(domain -> {
            try {
                OpenFGAClient client = clientFactory.get(domain);
                ApiResponse<ListStoresResponse> response = client.listStoresWithHttpInfo();
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    builder.withDetail(domain, Map.of(
                            "storeId", client.getStoreId(),
                            "status", Status.UP.getCode()));
                } else {
                    builder.withDetail(domain, Status.DOWN);
                    anyDown.setValue(true);
                }
            } catch (Exception e) {
                builder.withDetail(domain, Status.DOWN).withException(e);
                anyDown.setValue(true);
            }
        });

        builder.status(anyDown.getValue() ? Status.DOWN : Status.UP);

        return builder.build();
    }
}
