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
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.rest.api.service.SAML2SPService;

@WebServlet(name = "login", urlPatterns = { "/saml2sp/login" })
public class Login extends AbstractSAML2SPServlet {

    private static final long serialVersionUID = 968480296813639041L;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        String idp = request.getParameter(Constants.PARAM_IDP);

        SyncopeClient anonymous = (SyncopeClient) request.getServletContext().
                getAttribute(Constants.SYNCOPE_ANONYMOUS_CLIENT);
        try {
            SAML2RequestTO requestTO = anonymous.getService(SAML2SPService.class).createLoginRequest(
                    StringUtils.substringBefore(request.getRequestURL().toString(), "/saml2sp"), idp);

            prepare(response, requestTO);
        } catch (Exception e) {
            LOG.error("While preparing authentication request to IdP", e);

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
