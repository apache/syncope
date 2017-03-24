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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "userSelfPasswordReset", path = "/api/self/requestPasswordReset")
public class UserSelfPasswordReset extends BaseResource {

    private static final long serialVersionUID = -2721621682300247583L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            Map<String, String[]> parameters = request.getParameterMap();
            String[] usernameParam = parameters.get("username");
            if (ArrayUtils.isEmpty(usernameParam)) {
                throw new Exception("A valid username should be provided");
            }

            if (request.getHeader("captcha") == null
                    || !captchaCheck(
                            request.getHeader("captcha"),
                            request.getSession().getAttribute(SyncopeEnduserConstants.CAPTCHA_SESSION_KEY))) {

                throw new IllegalArgumentException("Entered captcha is not matching");
            }

            if (SyncopeEnduserSession.get().getPlatformInfo().isPwdResetRequiringSecurityQuestions()) {
                String[] securityAnswerParam = parameters.get("securityAnswer");
                if (ArrayUtils.isEmpty(securityAnswerParam)) {
                    throw new Exception("A correct security answer should be provided");
                }
                SyncopeEnduserSession.get().getService(UserSelfService.class).
                        requestPasswordReset(usernameParam[0], securityAnswerParam[0]);
            } else {
                SyncopeEnduserSession.get().getService(UserSelfService.class).
                        requestPasswordReset(usernameParam[0], null);
            }
            final String responseMessage = new StringBuilder().
                    append("Password reset request sent for user ").append(usernameParam[0]).toString();

            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (final Exception e) {
            LOG.error("Error while updating user", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(),
                    new StringBuilder().
                            append("ErrorMessage{{ ").
                            append(e.getMessage()).
                            append(" }}").
                            toString());
        }
        return response;
    }

}
