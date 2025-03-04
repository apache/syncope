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

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.lib.WebClientBuilder;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.AMSession;

public final class SRASessionRestClient extends AMSessionRestClient {

    private static final long serialVersionUID = 22118820292494L;

    public SRASessionRestClient(final List<NetworkService> list) {
        super(list);
    }

    @Override
    protected String getActuatorEndpoint() {
        return instances.getFirst().getAddress() + "actuator/sraSessions";
    }

    @Override
    public List<AMSession> list() {
        try {
            Response response = WebClientBuilder.build(getActuatorEndpoint(),
                    SyncopeWebApplication.get().getAnonymousUser(),
                    SyncopeWebApplication.get().getAnonymousKey(),
                    JAX_RS_PROVIDERS).accept(MediaType.APPLICATION_JSON_TYPE).get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(new GenericType<>() {
                });
            }

            LOG.error("Unexpected response for SSO Sessions from {}: {}",
                    getActuatorEndpoint(), response.getStatus());
        } catch (Exception e) {
            LOG.error("Could not fetch SSO Sessions from {}", getActuatorEndpoint(), e);
        }

        return List.of();
    }
}
