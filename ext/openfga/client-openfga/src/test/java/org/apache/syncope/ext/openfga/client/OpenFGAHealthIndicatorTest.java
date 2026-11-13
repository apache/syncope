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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.ext.openfga.client.model.ListStoresResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
class OpenFGAHealthIndicatorTest {

    private static DomainHolder<Object> domainHolder(final Map<String, Object> domains) {
        return new DomainHolder<Object>() {

            @Override
            public Map<String, Object> getDomains() {
                return domains;
            }

            @Override
            public Map<String, Boolean> getHealthInfo() {
                return Map.of();
            }
        };
    }

    private static ApiResponse<ListStoresResponse> response(final int statusCode) {
        return new ApiResponse<>(statusCode, Map.of(), new ListStoresResponse().stores(List.of()));
    }

    @Mock
    private OpenFGAClientFactory clientFactory;

    @Mock
    private OpenFGAClient client;

    private OpenFGAHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new OpenFGAHealthIndicator(
                domainHolder(Map.of(SyncopeConstants.MASTER_DOMAIN, new Object())),
                clientFactory);
    }

    @Test
    void up() throws Exception {
        when(clientFactory.getBaseUri()).thenReturn("http://openfga:8080");
        when(clientFactory.get(SyncopeConstants.MASTER_DOMAIN)).thenReturn(client);
        when(client.getStoreId()).thenReturn("store");
        when(client.listStoresWithHttpInfo()).thenReturn(response(200));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("baseUri")).isEqualTo("http://openfga:8080");
        assertThat(health.getDetails().get(SyncopeConstants.MASTER_DOMAIN)).
                isEqualTo(Map.of("storeId", "store", "status", Status.UP.getCode()));
    }

    @Test
    void nonSuccessfulStatusCodeMarksDomainDown() throws Exception {
        when(clientFactory.getBaseUri()).thenReturn("http://openfga:8080");
        when(clientFactory.get(SyncopeConstants.MASTER_DOMAIN)).thenReturn(client);
        when(client.listStoresWithHttpInfo()).thenReturn(response(503));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get(SyncopeConstants.MASTER_DOMAIN)).isEqualTo(Status.DOWN);
    }

    @Test
    void failureMarksDomainDown() throws Exception {
        when(clientFactory.getBaseUri()).thenReturn("http://openfga:8080");
        when(clientFactory.get(SyncopeConstants.MASTER_DOMAIN)).thenReturn(client);
        when(client.listStoresWithHttpInfo()).thenThrow(new ApiException("boom"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get(SyncopeConstants.MASTER_DOMAIN)).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
