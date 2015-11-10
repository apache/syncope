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
import javax.servlet.http.HttpServletRequest;
import org.apache.syncope.client.enduser.adapters.UserTOAdapter;
import org.apache.syncope.client.enduser.model.UserTORequest;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSelfCreateResource extends AbstractBaseResource {

    private static final long serialVersionUID = -2721621682300247583L;

    private static final Logger LOG = LoggerFactory.getLogger(UserSelfCreateResource.class);

    private final UserSelfService userSelfService;

    private final UserTOAdapter userTOAdapter;

    public UserSelfCreateResource() {
        userTOAdapter = new UserTOAdapter();
        userSelfService = getService(UserSelfService.class);
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {

        int responseStatus = 200;
        final StringBuilder responseMessage = new StringBuilder();
        ResourceResponse response = new ResourceResponse();

        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

            final UserTORequest userTORequest = POJOHelper.deserialize(IOUtils.toString(request.getInputStream()),
                    UserTORequest.class);

            if (isSelfRegistrationAllowed() && userTORequest != null) {
                LOG.debug("Received user self registration request for user: [{}]", userTORequest.getUsername());
                LOG.trace("Received user self registration request is: [{}]", userTORequest);
                // adapt request and create user
                userSelfService.create(userTOAdapter.fromUserTORequest(userTORequest, null), true);
                responseMessage.append("User").append(userTORequest.getUsername()).append("created successfully");
            } else {
                responseMessage.append(userTORequest == null
                        ? "Request received is not valid"
                        : "Self registration not allowed");
                responseStatus = 403;
            }
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

        } catch (final Exception e) {
            LOG.error("Could not read userTO from request", e);
            responseStatus = 400;
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(e.getMessage());
                }
            });
        }

        response.setStatusCode(responseStatus);
        return response;
    }

}
