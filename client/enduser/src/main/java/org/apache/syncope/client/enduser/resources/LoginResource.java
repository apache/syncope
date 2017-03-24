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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.model.Credentials;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.io.IOUtils;

@Resource(key = "login", path = "/api/login")
public class LoginResource extends BaseResource {

    private static final long serialVersionUID = -7720997467070461915L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.TEXT_PLAIN);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            Credentials credentials = MAPPER.readValue(
                    IOUtils.toString(request.getInputStream()), Credentials.class);
            final String username = credentials.getUsername();
            final String password = credentials.getPassword().isEmpty() ? null : credentials.getPassword();

            LOG.debug("Enduser login, user: {}", username);

            if (StringUtils.isBlank(username)) {
                LOG.error("Could not read credentials from request: username is blank!");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(),
                        "ErrorMessage{{ Could not read credentials from request: username is blank! }}");
            } else if (!SyncopeEnduserApplication.get().getAdminUser().equalsIgnoreCase(username)
                    && SyncopeEnduserSession.get().authenticate(username, password)) {
                // user has been authenticated successfully
                response.setTextEncoding(StandardCharsets.UTF_8.name());
                response.setWriteCallback(new WriteCallback() {

                    @Override
                    public void writeData(final Attributes attributes) throws IOException {
                        attributes.getResponse().write(username);
                    }
                });
                response.setStatusCode(Response.Status.OK.getStatusCode());
            } else {
                // not authenticated
                response.setError(Response.Status.UNAUTHORIZED.getStatusCode(),
                        "ErrorMessage{{ Username or password are incorrect }}");
            }
        } catch (Exception e) {
            LOG.error("Could not read credentials from request", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }
        return response;
    }

}
