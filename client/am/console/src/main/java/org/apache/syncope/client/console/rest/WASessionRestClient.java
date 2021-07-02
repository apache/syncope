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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WASessionRestClient implements RestClient {

    private static final long serialVersionUID = 22118820292494L;

    private static final Logger LOG = LoggerFactory.getLogger(WASessionRestClient.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String getActuatorEndpoint(final List<NetworkService> instances) {
        return instances.get(0).getAddress() + "actuator/ssoSessions";
    }

    public static List<WASession> list(final List<NetworkService> waInstances) {
        try {
            Response response = WebClient.create(
                    getActuatorEndpoint(waInstances),
                    SyncopeWebApplication.get().getAnonymousUser(),
                    SyncopeWebApplication.get().getAnonymousKey(),
                    null).
                    accept(MediaType.APPLICATION_JSON_TYPE).get();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                JsonNode node = MAPPER.readTree((InputStream) response.getEntity());
                if (node.has("activeSsoSessions")) {
                    return MAPPER.readValue(
                            MAPPER.treeAsTokens(node.get("activeSsoSessions")),
                            new TypeReference<List<WASession>>() {
                    });
                }
            } else {
                LOG.error("Unexpected response for SSO Sessions from {}: {}",
                        getActuatorEndpoint(waInstances), response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Could not fetch SSO Sessions from {}", getActuatorEndpoint(waInstances), e);
        }

        return List.of();
    }

    public static void delete(final List<NetworkService> waInstances, final String ticketGrantingTicket) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);

        try {
            Response response = WebClient.create(
                    getActuatorEndpoint(waInstances),
                    SyncopeWebApplication.get().getAnonymousUser(),
                    SyncopeWebApplication.get().getAnonymousKey(),
                    null).
                    path(ticketGrantingTicket).
                    accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).delete();
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOG.error("Unexpected response when deleting SSO Session {} from {}: {}",
                        ticketGrantingTicket, getActuatorEndpoint(waInstances), response.getStatus());
                sce.getElements().add("Unexpected response code: " + response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Could not delete SSO Session {} from {}",
                    ticketGrantingTicket, getActuatorEndpoint(waInstances), e);
            sce.getElements().add("Unexpected error: " + e.getMessage());
        }

        if (!sce.getElements().isEmpty()) {
            throw sce;
        }
    }

    private WASessionRestClient() {
        // private constructor for static utility class
    }
}
