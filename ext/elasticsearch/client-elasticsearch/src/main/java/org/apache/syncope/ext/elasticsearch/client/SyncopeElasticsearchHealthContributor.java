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
package org.apache.syncope.ext.elasticsearch.client;

import static co.elastic.clients.elasticsearch._types.HealthStatus.Green;
import static co.elastic.clients.elasticsearch._types.HealthStatus.Red;
import static co.elastic.clients.elasticsearch._types.HealthStatus.Yellow;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class SyncopeElasticsearchHealthContributor implements HealthIndicator {

    protected final ElasticsearchClient client;

    public SyncopeElasticsearchHealthContributor(final ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            HealthResponse health = client.cluster().health();
            switch (health.status()) {
                case Green:
                case Yellow:
                    builder.up();
                    break;

                case Red:
                default:
                    builder.down();
            }
            builder.withDetail("cluster_name", health.clusterName());
            builder.withDetail("status", health.status().jsonValue());
            builder.withDetail("timed_out", health.timedOut());
            builder.withDetail("number_of_nodes", health.numberOfNodes());
            builder.withDetail("number_of_data_nodes", health.numberOfDataNodes());
            builder.withDetail("active_primary_shards", health.activePrimaryShards());
            builder.withDetail("relocating_shards", health.relocatingShards());
            builder.withDetail("initializing_shards", health.initializingShards());
            builder.withDetail("unassigned_shards", health.unassignedShards());
            builder.withDetail("delayed_unassigned_shards", health.delayedUnassignedShards());
            builder.withDetail("number_of_pending_tasks", health.numberOfPendingTasks());
            builder.withDetail("number_of_in_flight_fetch", health.numberOfInFlightFetch());
            builder.withDetail("task_max_waiting_in_queue_millis", health.taskMaxWaitingInQueueMillis());
            builder.withDetail("active_shards_percent_as_number", health.activeShardsPercentAsNumber());
        } catch (Exception e) {
            builder.down(e);
        }

        return builder.build();
    }
}
