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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "assertionConsumer", urlPatterns = { "/saml2sp/assertion-consumer" })
public class AssertionConsumer extends HttpServlet {

    private static final long serialVersionUID = 968480296813639041L;

    private static final Logger LOG = LoggerFactory.getLogger(AssertionConsumer.class);

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        SyncopeClient anonymous = (SyncopeClient) request.getServletContext().
                getAttribute(Constants.SYNCOPE_ANONYMOUS_CLIENT);
        try {
            SAML2LoginResponseTO responseTO = anonymous.getService(SAML2SPService.class).
                    validateLoginResponse(request.getInputStream());

            request.getSession(true).setAttribute(Constants.SAML2SPJWT, responseTO.getAccessToken());

            String successURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGIN_SUCCESS_URL);
            if (successURL == null) {
                request.setAttribute("responseTO", responseTO);
                request.getRequestDispatcher("loginSuccess.jsp").forward(request, response);
            } else {
                response.sendRedirect(successURL + "?sloSupported=" + responseTO.isSloSupported());
            }
        } catch (Exception e) {
            LOG.error("While processing authentication response from IdP", e);

            String errorURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGIN_ERROR_URL);
            if (errorURL == null) {
                request.setAttribute("exception", e);
                request.getRequestDispatcher("loginError.jsp").forward(request, response);

                e.printStackTrace(response.getWriter());
            } else {
                response.sendRedirect(errorURL + "?errorMessage="
                        + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8.name()));
            }
        }
    }
}
