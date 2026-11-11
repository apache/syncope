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
package org.apache.syncope.ext.opensearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
public class SyncopeOpenSearchHealthContributorTest {

    @Mock
    private OpenSearchClient client;

    @Mock
    private OpenSearchClusterClient cluster;

    private SyncopeOpenSearchHealthContributor contributor;

    @BeforeEach
    public void setupContributor() {
        contributor = new SyncopeOpenSearchHealthContributor(client);
    }

    private static HealthResponse healthResponse(final HealthStatus status) {
        return HealthResponse.of(b -> b.
                clusterName("syncope").
                status(status).
                timedOut(false).
                numberOfNodes(2).
                numberOfDataNodes(2).
                activePrimaryShards(3).
                activeShards(6).
                relocatingShards(0).
                initializingShards(0).
                unassignedShards(0).
                delayedUnassignedShards(0).
                numberOfPendingTasks(1).
                numberOfInFlightFetch(0).
                taskMaxWaitingInQueueMillis(0).
                activeShardsPercentAsNumber(100));
    }

    @Test
    public void up() throws Exception {
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health()).thenReturn(healthResponse(HealthStatus.Green));

        Health health = contributor.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).
                containsEntry("cluster_name", "syncope").
                containsEntry("status", "green").
                containsEntry("timed_out", false).
                containsEntry("number_of_nodes", 2).
                containsEntry("number_of_data_nodes", 2).
                containsEntry("active_primary_shards", 3).
                containsEntry("active_shards_percent_as_number", 100.0);
    }

    @Test
    public void upWithYellowStatus() throws Exception {
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health()).thenReturn(healthResponse(HealthStatus.Yellow));

        assertThat(contributor.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    public void downWithRedStatus() throws Exception {
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health()).thenReturn(healthResponse(HealthStatus.Red));

        Health health = contributor.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "red");
    }

    @Test
    public void downOnFailure() throws Exception {
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health()).thenThrow(new IOException("boom"));

        Health health = contributor.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
