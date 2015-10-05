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
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.adapters.UserTOAdapter;
import org.apache.syncope.client.enduser.model.UserTORequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSelfUpdateResource extends AbstractBaseResource {

    private static final long serialVersionUID = -2721621682300247583L;

    private static final Logger LOG = LoggerFactory.getLogger(UserSelfUpdateResource.class);

    private final UserSelfService userSelfService;

    private final UserTOAdapter userTOAdapter;

    public UserSelfUpdateResource() {
        userTOAdapter = new UserTOAdapter();
        userSelfService = getService(UserSelfService.class);
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {

        int responseStatus = 200;
        final String responseMessage;
        ResourceResponse response = new ResourceResponse();

        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

            final UserTORequest userTOResponse = POJOHelper.deserialize(IOUtils.toString(request.getInputStream()),
                    UserTORequest.class);

            LOG.debug("userTOResponse: {}", userTOResponse);

            // adapt user, change self password only value passed is not null and has changed
            UserTO userTO = userTOAdapter.fromUserTORequest(userTOResponse, SyncopeEnduserSession.get().getPassword());

            LOG.debug("Enduser user self update, user: {}", userTO.toString());

            // update user
            userSelfService.update(userTO);
            responseMessage = "User updated successfully";

            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

        } catch (final Exception e) {
            responseStatus = 400;
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(e.getMessage());
                }
            });
            LOG.error("Could not read userTO from request", e);
        }

        response.setStatusCode(responseStatus);
        return response;
    }

}
