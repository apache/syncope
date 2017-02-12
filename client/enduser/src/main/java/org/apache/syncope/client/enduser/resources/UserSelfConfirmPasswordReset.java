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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

public class UserSelfConfirmPasswordReset extends AbstractBaseResource {

    private static final long serialVersionUID = -2721621682300247583L;

    private final UserSelfService userSelfService;

    public UserSelfConfirmPasswordReset() {
        userSelfService = SyncopeEnduserSession.get().getService(UserSelfService.class);
    }

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        AbstractResource.ResourceResponse response = new AbstractResource.ResourceResponse();

        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            Map<String, String[]> parameters = request.getParameterMap();
            String token;
            if (parameters.get("token") == null || parameters.get("token").length == 0) {
                throw new Exception("A valid token should be provided");
            } else {
                token = parameters.get("token")[0];
            }

            if (parameters.get("newPassword") == null || parameters.get("newPassword").length == 0) {
                throw new Exception("A new correct password should be provided");
            }
            userSelfService.confirmPasswordReset(token, parameters.get("newPassword")[0]);

            final String responseMessage = new StringBuilder().append("Password changed correctly").toString();

            response.setTextEncoding(SyncopeConstants.DEFAULT_ENCODING);
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

            response.setStatusCode(Response.Status.OK.getStatusCode());

        } catch (final Exception e) {
            LOG.error("Error while updating user", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }
        return response;
    }

}
