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

import static org.apache.syncope.client.enduser.resources.BaseResource.LOG;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.string.StringValue;

@Resource(key = "userRequestCancel", path = "/api/flowable/userRequests/${executionId}")
public class UserRequestCancelResource extends BaseResource {

    private static final long serialVersionUID = 7273151109078469253L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {

        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        StringValue executionId;
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            if (!HttpMethod.DELETE.equals(request.getMethod())) {
                throw new UnsupportedOperationException("Unsupported operation, only DELETE allowed");
            }

            PageParameters parameters = attributes.getParameters();
            executionId = parameters.get("executionId");
            IRequestParameters requestParameters = attributes.getRequest().getQueryParameters();
            StringValue reason = requestParameters.getParameterValue("reason");
            LOG.debug("Cancel Flowable User Request with execution id [{}] for user [{}] with reason [{}]", executionId,
                    SyncopeEnduserSession.get().getSelfTO().getUsername(), reason);
            if (executionId.isEmpty()) {
                throw new IllegalArgumentException("Empty executionId, please provide a value");
            }

            SyncopeEnduserSession.get().getService(UserRequestService.class).cancel(executionId.toString(),
                    reason.toString());

            final String outcomeMessage = String.format(
                    "User Request with execution id [%s] successfully canceled for User [%s]", executionId.
                            toString(), SyncopeEnduserSession.get().getSelfTO().getUsername());

            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final IResource.Attributes attributes) throws IOException {
                    attributes.getResponse().write(outcomeMessage);
                }
            });

            response.setContentType(MediaType.APPLICATION_JSON);
            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error cancelling User Request for [{}]", SyncopeEnduserSession.get().getSelfTO().getUsername(),
                    e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }

        return response;
    }
}
