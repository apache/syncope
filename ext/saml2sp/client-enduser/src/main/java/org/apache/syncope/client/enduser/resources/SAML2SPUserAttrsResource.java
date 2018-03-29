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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.ext.saml2lsp.agent.Constants;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "saml2SPs", path = "/api/saml2SPs/userAttrs")
public class SAML2SPUserAttrsResource extends BaseResource {

    private static final long serialVersionUID = 7273151109078469253L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        ResourceResponse response = new ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);

        try {
            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(
                            (CharSequence) ((HttpServletRequest) RequestCycle.get().getRequest().
                                    getContainerRequest()).getSession().getAttribute(Constants.SAML2SP_USER_ATTRS));
                }
            });
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving saml2 user attributes", e);
            response.setError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }

        return response;
    }
}
