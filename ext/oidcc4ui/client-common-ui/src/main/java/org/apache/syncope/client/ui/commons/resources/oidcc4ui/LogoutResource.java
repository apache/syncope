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
package org.apache.syncope.client.ui.commons.resources.oidcc4ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.common.lib.oidc.OIDCConstants;
import org.apache.syncope.common.rest.api.service.OIDCC4UIService;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LogoutResource extends AbstractResource {

    private static final long serialVersionUID = 273797583932923564L;

    protected static final Logger LOG = LoggerFactory.getLogger(LogoutResource.class);

    protected abstract Class<? extends WebPage> getLogoutPageClass();

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

        // if no logout token was found, complete RP-initated logout
        // otherwise, proceed with back-channel logout for the provided token
        String logoutToken = Optional.ofNullable(request.getParameter(OIDCConstants.LOGOUT_TOKEN)).
                orElseThrow(() -> new RestartResponseException(getLogoutPageClass(), new PageParameters()));

        OIDCC4UIService service = BaseSession.class.cast(Session.get()).getAnonymousService(OIDCC4UIService.class);

        ResourceResponse response = new ResourceResponse();
        response.getHeaders().addHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.getHeaders().addHeader("Pragma", "no-cache");
        try {
            service.backChannelLogout(logoutToken, request.getRequestURL().toString());

            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("While requesting back-channel logout for token {}", logoutToken, e);

            response.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
            response.setContentType(MediaType.APPLICATION_JSON);
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes atrbts) throws IOException {
                    atrbts.getResponse().write("{\"error\": \"" + e.getMessage() + "\"}");
                }
            });
        }
        return response;
    }
}
