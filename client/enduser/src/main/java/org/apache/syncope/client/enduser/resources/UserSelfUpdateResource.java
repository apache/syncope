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
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.adapters.UserTOAdapter;
import org.apache.syncope.client.enduser.model.UserTORequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
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

            final UserTORequest userTOResponse = POJOHelper.deserialize(IOUtils.toString(request.getInputStream()),
                    UserTORequest.class);

            LOG.trace("userTOResponse: {}", userTOResponse);

            // adapt user, change self password only value passed is not null and has changed
            UserTO userTO = userTOAdapter.fromUserTORequest(userTOResponse, SyncopeEnduserSession.get().getPassword());

            LOG.debug("User {} id updating himself", userTO.getUsername());

            // update user
            Response res = userSelfService.update(userTO);

            final String responseMessage = res.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)
                            ? new StringBuilder().append("User").append(userTO.getUsername()).append(
                            " successfully updated").toString()
                            : new StringBuilder().append("ErrorMessage{{ ").
                            append(res.getStatusInfo().getReasonPhrase()).append(" }}").toString();

            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

            response.setStatusCode(res.getStatus());

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
