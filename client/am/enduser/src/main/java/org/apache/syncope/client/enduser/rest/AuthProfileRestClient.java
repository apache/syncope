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
package org.apache.syncope.client.enduser.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.lang3.Strings;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.rest.api.service.AuthProfileSelfService;

public class AuthProfileRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4139153766778113329L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    public AuthProfileTO read() {
        try {
            return getService(AuthProfileSelfService.class).read();
        } catch (Exception e) {
            LOG.debug("While attempting to read the auth profile", e);
            return null;
        }
    }

    public void update(final AuthProfileTO authProfile) {
        getService(AuthProfileSelfService.class).update(authProfile);
    }

    public void delete() {
        getService(AuthProfileSelfService.class).delete();
    }

    public String readConsentAttributes(final NetworkService service, final String principal, final long id)
            throws IOException {

        Response response = WebClient.create(
                Strings.CS.appendIfMissing(service.getAddress(), "/") + "actuator/attributeConsent/" + principal,
                List.of(),
                SyncopeWebApplication.get().getAnonymousUser(),
                SyncopeWebApplication.get().getAnonymousKey(),
                null).accept(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonNode nodes = MAPPER.readTree((InputStream) response.getEntity());
            for (JsonNode node : nodes) {
                if (node.has("decision")) {
                    JsonNode decision = node.get("decision");
                    if (decision.has("id") && id == decision.get("id").asLong()) {
                        if (node.has("attributes")) {
                            return node.get("attributes").toString();
                        }
                    }
                }
            }
        } else {
            LOG.error("While contacting the /actuator/attributeConsent endpoint: HTTP {}", response.getStatus());
        }

        return "{}";
    }
}
