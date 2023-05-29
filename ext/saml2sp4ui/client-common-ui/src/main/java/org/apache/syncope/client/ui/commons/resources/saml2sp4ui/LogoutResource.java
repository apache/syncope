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
package org.apache.syncope.client.ui.commons.resources.saml2sp4ui;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.common.lib.saml2.SAML2Constants;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class LogoutResource extends AbstractSAML2SP4UIResource {

    private static final long serialVersionUID = 4865223550672539533L;

    protected abstract Class<? extends WebPage> getLogoutPageClass();

    protected ResourceResponse doLogout(final SAML2Response saml2Response) {
        SAML2SP4UIService service = BaseSession.class.cast(Session.get()).getAnonymousService(SAML2SP4UIService.class);
        service.validateLogoutResponse(saml2Response);

        throw new RestartResponseException(getLogoutPageClass(), new PageParameters());
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
        HttpServletResponse response = (HttpServletResponse) attributes.getResponse().getContainerResponse();

        switch (request.getMethod()) {
            case HttpMethod.GET -> {
                String samlResponse = request.getParameter(SAML2Constants.SAML_RESPONSE);
                String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
                if (samlResponse == null) {
                    // create logout request
                    Cookie idpEntityID = new Cookie(
                            SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID,
                            request.getParameter(SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID));
                    idpEntityID.setMaxAge(-1);
                    response.addCookie(idpEntityID);

                    SAML2SP4UIService service =
                            BaseSession.class.cast(Session.get()).getService(SAML2SP4UIService.class);
                    SAML2Request logoutRequest = service.createLogoutRequest(
                            spEntityID(attributes), SAML2SP4UIConstants.URL_CONTEXT);

                    Session.get().invalidate();

                    return send(logoutRequest);
                } else {
                    // process REDIRECT binding logout response
                    return doLogout(buildResponse(attributes, samlResponse, relayState));
                }
            }

            case HttpMethod.POST -> {
                return doLogout(extract(attributes));
            }

            default -> throw new UnsupportedOperationException("Only GET and POST are supported");
        }
    }
}
