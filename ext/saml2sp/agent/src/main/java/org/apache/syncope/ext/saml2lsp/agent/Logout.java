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
package org.apache.syncope.ext.saml2lsp.agent;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SSOConstants;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.rest.api.service.SAML2SPService;

@WebServlet(name = "logout", urlPatterns = { "/saml2sp/logout" })
public class Logout extends AbstractSAML2SPServlet {

    private static final long serialVersionUID = 3010286040376932117L;

    private void doLogout(
            final SAML2ReceivedResponseTO receivedResponse,
            final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        SyncopeClientFactoryBean clientFactory = (SyncopeClientFactoryBean) request.getServletContext().
                getAttribute(Constants.SYNCOPE_CLIENT_FACTORY);
        try {
            String accessToken = (String) request.getSession().getAttribute(Constants.SAML2SPJWT);
            if (StringUtils.isBlank(accessToken)) {
                throw new IllegalArgumentException("No access token found ");
            }

            SyncopeClient client = clientFactory.create(accessToken);
            client.getService(SAML2SPService.class).validateLogoutResponse(receivedResponse);

            String successURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGOUT_SUCCESS_URL);
            if (successURL == null) {
                request.getRequestDispatcher("logoutSuccess.jsp").forward(request, response);
            } else {
                response.sendRedirect(successURL);
            }
            request.getSession().removeAttribute(Constants.SAML2SPJWT);
        } catch (Exception e) {
            LOG.error("While processing authentication response from IdP", e);

            String errorURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGOUT_ERROR_URL);
            if (errorURL == null) {
                request.setAttribute("exception", e);
                request.getRequestDispatcher("logoutError.jsp").forward(request, response);

                e.printStackTrace(response.getWriter());
            } else {
                response.sendRedirect(errorURL + "?errorMessage="
                        + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8.name()));
            }
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        String samlResponse = request.getParameter(SSOConstants.SAML_RESPONSE);
        String relayState = request.getParameter(SSOConstants.RELAY_STATE);
        if (samlResponse == null) { // prepare logout response
            SyncopeClientFactoryBean clientFactory = (SyncopeClientFactoryBean) request.getServletContext().
                    getAttribute(Constants.SYNCOPE_CLIENT_FACTORY);
            try {
                String accessToken = (String) request.getSession().getAttribute(Constants.SAML2SPJWT);
                if (StringUtils.isBlank(accessToken)) {
                    throw new IllegalArgumentException("No access token found ");
                }

                SyncopeClient client = clientFactory.create(accessToken);
                SAML2RequestTO requestTO = client.getService(SAML2SPService.class).createLogoutRequest(
                        StringUtils.substringBefore(request.getRequestURL().toString(), "/saml2sp"));

                prepare(response, requestTO);
            } catch (Exception e) {
                LOG.error("While preparing logout request to IdP", e);

                String errorURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGOUT_ERROR_URL);
                if (errorURL == null) {
                    request.setAttribute("exception", e);
                    request.getRequestDispatcher("logoutError.jsp").forward(request, response);

                    e.printStackTrace(response.getWriter());
                } else {
                    response.sendRedirect(errorURL + "?errorMessage="
                            + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8.name()));
                }
            }
        } else { // process REDIRECT binding logout response
            SAML2ReceivedResponseTO receivedResponse = new SAML2ReceivedResponseTO();
            receivedResponse.setSamlResponse(samlResponse);
            receivedResponse.setRelayState(relayState);
            receivedResponse.setBindingType(SAML2BindingType.REDIRECT);

            doLogout(receivedResponse, request, response);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        // process POST binding logout response
        SAML2ReceivedResponseTO receivedResponse = extract(request.getInputStream());
        receivedResponse.setBindingType(SAML2BindingType.POST);
        doLogout(receivedResponse, request, response);
    }

}
