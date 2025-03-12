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
package org.apache.syncope.client.console.rest;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.lib.WebClientBuilder;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SRAStatisticsRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(SRAStatisticsRestClient.class);

    private static final List<?> JAX_RS_PROVIDERS =
            List.of(new JacksonJsonProvider(JsonMapper.builder().findAndAddModules().build()));

    protected String getActuatorEndpoint(final List<NetworkService> instances) {
        return instances.getFirst().getAddress() + "actuator/metrics/spring.cloud.gateway.requests";
    }

    public SRAStatistics get(final List<NetworkService> instances, final List<Pair<String, String>> selected) {
        try {
            WebClient client = WebClientBuilder.build(getActuatorEndpoint(instances),
                    SyncopeWebApplication.get().getAnonymousUser(),
                    SyncopeWebApplication.get().getAnonymousKey(),
                    JAX_RS_PROVIDERS).accept(MediaType.APPLICATION_JSON_TYPE);

            if (!selected.isEmpty()) {
                client.query("tag", selected.stream().map(s -> s.getKey() + ":" + s.getValue()).toArray());
            }

            Response response = client.get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(SRAStatistics.class);
            }

            LOG.error("Unexpected response for SRA statistics from {}: {}",
                    getActuatorEndpoint(instances), response.getStatus());
        } catch (Exception e) {
            LOG.error("Could not fetch SRA statistics from {}", getActuatorEndpoint(instances), e);
        }

        return new SRAStatistics();
    }
}
