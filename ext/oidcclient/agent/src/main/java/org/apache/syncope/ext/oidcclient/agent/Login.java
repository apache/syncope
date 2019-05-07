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
package org.apache.syncope.ext.oidcclient.agent;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.OIDCConstants;
import org.apache.syncope.common.lib.to.OIDCLoginRequestTO;
import org.apache.syncope.common.rest.api.service.OIDCClientService;
import org.springframework.context.ApplicationContext;

public class Login extends AbstractOIDCClientServlet {

    private static final long serialVersionUID = 968480296813639041L;

    private final String anonymousUser;

    private final String anonymousKey;

    private final boolean useGZIPCompression;

    public Login(final ApplicationContext ctx,
            final String anonymousUser,
            final String anonymousKey,
            final boolean useGZIPCompression) {

        super(ctx);
        this.anonymousUser = anonymousUser;
        this.anonymousKey = anonymousKey;
        this.useGZIPCompression = useGZIPCompression;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        String op = request.getParameter(Constants.PARAM_OP);

        SyncopeClient anonymous =
                getAnonymousClient(request.getServletContext(), anonymousUser, anonymousKey, useGZIPCompression);
        try {
            String redirectURI = StringUtils.substringBefore(request.getRequestURL().toString(), "/login")
                    + "/code-consumer";
            OIDCLoginRequestTO requestTO = anonymous.getService(OIDCClientService.class).
                    createLoginRequest(redirectURI, op);

            request.getSession().setAttribute(OIDCConstants.STATE, requestTO.getState());
            request.getSession().setAttribute(OIDCConstants.OP, op);

            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
            response.setHeader("Pragma", "no-cache");
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);

            UriBuilder ub = UriBuilder.fromUri(requestTO.getProviderAddress());
            ub.queryParam(OIDCConstants.CLIENT_ID, requestTO.getClientId());
            ub.queryParam(OIDCConstants.REDIRECT_URI, requestTO.getRedirectURI());
            ub.queryParam(OIDCConstants.RESPONSE_TYPE, requestTO.getResponseType());
            ub.queryParam(OIDCConstants.SCOPE, requestTO.getScope());
            ub.queryParam(OIDCConstants.STATE, requestTO.getState());
            response.setHeader(HttpHeaders.LOCATION, ub.build().toASCIIString());
        } catch (Exception e) {
            LOG.error("While preparing the Authentication Request", e);

            String errorURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGIN_ERROR_URL);
            if (errorURL == null) {
                request.setAttribute("exception", e);
                request.getRequestDispatcher("loginError.jsp").forward(request, response);

                e.printStackTrace(response.getWriter());
            } else {
                response.sendRedirect(errorURL + "?errorMessage="
                        + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            }
        }
    }
}
