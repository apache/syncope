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
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.syncope.common.lib.oidc.OIDCConstants;
import org.apache.syncope.common.lib.oidc.OIDCRequest;
import org.apache.syncope.common.rest.api.service.OIDCC4UIService;
import org.apache.wicket.Session;
import org.apache.wicket.request.resource.AbstractResource;

@Resource(
        key = OIDCC4UIConstants.URL_CONTEXT + ".login",
        path = "/" + OIDCC4UIConstants.URL_CONTEXT + "/login")
public class LoginResource extends AbstractResource {

    private static final long serialVersionUID = -3076690953674174306L;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        String op = attributes.getRequest().getQueryParameters().
                getParameterValue(OIDCC4UIConstants.PARAM_OP).toString();

        HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
        String redirectURI = StringUtils.substringBefore(
                request.getRequestURL().toString(), "/login") + "/code-consumer";

        OIDCC4UIService service = BaseSession.class.cast(Session.get()).getAnonymousService(OIDCC4UIService.class);
        OIDCRequest loginRequest = service.createLoginRequest(redirectURI, op);

        Session.get().setAttribute(OIDCConstants.OP, op);

        ResourceResponse response = new ResourceResponse();
        response.setStatusCode(Response.Status.FOUND.getStatusCode());
        response.getHeaders().addHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.getHeaders().addHeader("Pragma", "no-cache");
        response.getHeaders().addHeader(HttpHeaders.LOCATION, loginRequest.getLocation());

        return response;
    }
}
