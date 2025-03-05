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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.lib.WebClientBuilder;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.AMSession;

public final class WASessionRestClient extends AMSessionRestClient {

    private static final long serialVersionUID = 22118820292494L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().
            addModule(new SimpleModule().addDeserializer(AMSession.class, new AMSessionDeserializer())).build();

    public WASessionRestClient(final List<NetworkService> instances) {
        super(instances);
    }

    @Override
    protected String getActuatorEndpoint() {
        return instances.getFirst().getAddress() + "actuator/ssoSessions";
    }

    @Override
    public List<AMSession> list() {
        try {
            Response response = WebClientBuilder.build(getActuatorEndpoint(),
                    SyncopeWebApplication.get().getAnonymousUser(),
                    SyncopeWebApplication.get().getAnonymousKey(),
                    List.of()).accept(MediaType.APPLICATION_JSON_TYPE).get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                JsonNode node = MAPPER.readTree((InputStream) response.getEntity());
                if (node.has("activeSsoSessions")) {
                    return MAPPER.readValue(MAPPER.treeAsTokens(node.get("activeSsoSessions")), new TypeReference<>() {
                    });
                }
            } else {
                LOG.error("Unexpected response for SSO Sessions from {}: {}",
                        getActuatorEndpoint(), response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Could not fetch SSO Sessions from {}", getActuatorEndpoint(), e);
        }

        return List.of();
    }
}
