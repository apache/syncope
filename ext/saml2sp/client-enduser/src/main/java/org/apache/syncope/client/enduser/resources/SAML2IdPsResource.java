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
package org.apache.syncope.client.enduser.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.rest.api.service.SAML2IdPService;
import org.apache.wicket.request.resource.AbstractResource;

@Resource(key = "saml2IdPs", path = "/api/saml2IdPs")
public class SAML2IdPsResource extends BaseResource {

    private static final long serialVersionUID = -1538214102767503491L;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        ResourceResponse response = new ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setTextEncoding(StandardCharsets.UTF_8.name());
        try {
            final ArrayNode result = MAPPER.createArrayNode();

            for (SAML2IdPTO idp : SyncopeEnduserSession.get().getService(SAML2IdPService.class).list()) {
                ObjectNode idpNode = MAPPER.createObjectNode();
                idpNode.put("name", idp.getName());
                idpNode.put("entityID", idp.getEntityID());
                idpNode.put("logout", idp.isLogoutSupported());
                result.add(idpNode);
            }

            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(MAPPER.writeValueAsString(result));
                }
            });
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving available SAML 2.0 Identity Providers", e);
            response.setError(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "ErrorMessage{{ " + e.getMessage() + "}}");
        }

        return response;
    }

}
