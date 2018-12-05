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
import static org.apache.syncope.client.enduser.resources.BaseResource.MAPPER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.UserRequestFormPropertyType;
import org.apache.syncope.common.rest.api.beans.UserRequestFormQuery;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.string.StringValue;

@Resource(key = "userRequestsForms", path = "/api/flowable/userRequests/forms")
public class UserRequestsFormsResource extends BaseResource {

    private static final long serialVersionUID = 7273151109078469253L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {

        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setTextEncoding(StandardCharsets.UTF_8.name());
        StringValue username = StringValue.valueOf(SyncopeEnduserSession.get().getSelfTO().getUsername());
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            switch (request.getMethod()) {
                case HttpMethod.GET:
                    IRequestParameters requestParameters = attributes.getRequest().getQueryParameters();
                    StringValue page = requestParameters.getParameterValue("page");
                    StringValue size = requestParameters.getParameterValue("size");
                    LOG.debug("List available Flowable User Requests Forms by user [{}]", username);
                    final PagedResult<UserRequestForm> userRequestForms = SyncopeEnduserSession.get().
                            getService(UserRequestService.class).getForms(
                            new UserRequestFormQuery.Builder()
                                    .user(username.isEmpty()
                                            ? SyncopeEnduserSession.get().getSelfTO().getUsername()
                                            : username.toString())
                                    .page(page.isEmpty()
                                            ? 1
                                            : Integer.parseInt(page.toString()))
                                    .size(size.isEmpty()
                                            ? 10
                                            : Integer.parseInt(size.toString())).build());

                    // Date -> millis conversion for Date properties of the form
                    userRequestForms.getResult().stream().forEach(form
                            -> form.getProperties().stream()
                                    .filter(prop -> UserRequestFormPropertyType.Date == prop.getType()
                                    && StringUtils.isNotBlank(prop.getValue()))
                                    .forEach(prop -> {
                                        try {
                                            prop.setValue(String.valueOf(FastDateFormat.getInstance(prop.
                                                    getDatePattern()).parse(prop.getValue()).getTime()));
                                        } catch (ParseException e) {
                                            LOG.error("Unable to parse date", e);
                                        }
                                    }));

                    response.setWriteCallback(new AbstractResource.WriteCallback() {

                        @Override
                        public void writeData(final IResource.Attributes attributes) throws IOException {
                            attributes.getResponse().write(MAPPER.writeValueAsString(userRequestForms));
                        }
                    });
                    break;
                case HttpMethod.POST:
                    UserRequestForm requestForm = MAPPER.
                            readValue(request.getReader().readLine(), UserRequestForm.class);
                    if (requestForm == null) {
                        throw new IllegalArgumentException("Empty userRequestForm, please provide a valid one");
                    }

                    UserRequestService userRequestService = SyncopeEnduserSession.get().getService(
                            UserRequestService.class);
                    // 1. claim form as logged user
                    userRequestService.claimForm(requestForm.getTaskId());
                    // millis -> Date conversion for Date properties of the form
                    requestForm.getProperties().stream()
                            .filter(prop -> UserRequestFormPropertyType.Date == prop.getType()
                            && StringUtils.isNotBlank(prop.getValue()))
                            .forEach(prop -> {
                                try {
                                    prop.setValue(FastDateFormat.getInstance(prop.getDatePattern()).format(Long.valueOf(
                                            prop.getValue())));
                                } catch (NumberFormatException e) {
                                    LOG.error("Unable to format date", e);
                                }
                            });
                    // 2. Submit form
                    LOG.debug("Submit Flowable User Request Form for user [{}]", requestForm.getUsername());
                    userRequestService.submitForm(requestForm);

                    response.setStatusCode(Response.Status.NO_CONTENT.getStatusCode());
                    response.setWriteCallback(new AbstractResource.WriteCallback() {

                        @Override
                        public void writeData(final IResource.Attributes attributes) throws IOException {
                            // DO NOTHING
                        }
                    });
                    break;
                default:
                    LOG.error("Method [{}] not supported", request.getMethod());
                    response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                            .append("ErrorMessage{{ ")
                            .append("Method not supported")
                            .append(" }}")
                            .toString());
                    break;
            }
            response.setContentType(MediaType.APPLICATION_JSON);
            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error dealing with forms of user [{}]", username, e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }

        return response;
    }
}
