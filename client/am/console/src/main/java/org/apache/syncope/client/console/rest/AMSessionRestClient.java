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
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.AMSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AMSessionRestClient implements RestClient {

    private static final long serialVersionUID = 17371816842780L;

    protected static final Logger LOG = LoggerFactory.getLogger(AMSessionRestClient.class);

    protected static final List<?> JAX_RS_PROVIDERS =
            List.of(new JacksonJsonProvider(JsonMapper.builder().findAndAddModules().build()));

    protected final List<NetworkService> instances;

    protected AMSessionRestClient(final List<NetworkService> instances) {
        this.instances = instances;
    }

    protected abstract String getActuatorEndpoint();

    public abstract List<AMSession> list();

    public void delete(final String key) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);

        try {
            Response response = WebClient.create(
                    getActuatorEndpoint(),
                    SyncopeWebApplication.get().getAnonymousUser(),
                    SyncopeWebApplication.get().getAnonymousKey(),
                    null).
                    path(key).
                    accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).delete();
            if (response.getStatus() != Response.Status.OK.getStatusCode()
                    && response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {

                LOG.error("Unexpected response when deleting SSO Session {} from {}: {}",
                        key, getActuatorEndpoint(), response.getStatus());
                sce.getElements().add("Unexpected response code: " + response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Could not delete SSO Session {} from {}",
                    key, getActuatorEndpoint(), e);
            sce.getElements().add("Unexpected error: " + e.getMessage());
        }

        if (!sce.getElements().isEmpty()) {
            throw sce;
        }
    }
}
