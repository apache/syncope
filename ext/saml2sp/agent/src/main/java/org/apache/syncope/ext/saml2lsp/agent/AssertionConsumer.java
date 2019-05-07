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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.springframework.context.ApplicationContext;

public class AssertionConsumer extends AbstractSAML2SPServlet {

    private static final long serialVersionUID = 968480296813639041L;

    private static final ObjectMapper MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private final String anonymousUser;

    private final String anonymousKey;

    private final boolean useGZIPCompression;

    public AssertionConsumer(
            final ApplicationContext ctx,
            final String anonymousUser,
            final String anonymousKey,
            final boolean useGZIPCompression) {

        super(ctx);
        this.anonymousUser = anonymousUser;
        this.anonymousKey = anonymousKey;
        this.useGZIPCompression = useGZIPCompression;
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        SyncopeClient anonymous =
                getAnonymousClient(request.getServletContext(), anonymousUser, anonymousKey, useGZIPCompression);
        try {
            SAML2LoginResponseTO responseTO = anonymous.getService(SAML2SPService.class).
                    validateLoginResponse(extract(
                            StringUtils.substringBefore(request.getRequestURL().toString(), "/saml2sp"),
                            "saml2sp",
                            request.getRemoteAddr(),
                            request.getInputStream()));

            if (responseTO.isSelfReg()) {
                responseTO.getAttrs().add(new Attr.Builder("username").values(responseTO.getUsername()).build());
                request.getSession(true).
                        setAttribute(Constants.SAML2SP_USER_ATTRS, MAPPER.writeValueAsString(responseTO.getAttrs()));

                String selfRegRedirectURL =
                        getServletContext().getInitParameter(Constants.CONTEXT_PARAM_REDIRECT_SELFREG_URL);
                if (selfRegRedirectURL == null) {
                    request.setAttribute("responseTO", responseTO);
                    request.getRequestDispatcher("loginSuccess.jsp").forward(request, response);
                } else {
                    response.sendRedirect(selfRegRedirectURL);
                }
            } else {
                request.getSession(true).
                        setAttribute(Constants.SAML2SPJWT, responseTO.getAccessToken());
                request.getSession(true).
                        setAttribute(Constants.SAML2SPJWT_EXPIRE, responseTO.getAccessTokenExpiryTime());

                String successURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGIN_SUCCESS_URL);
                if (successURL == null) {
                    request.setAttribute("responseTO", responseTO);
                    request.getRequestDispatcher("loginSuccess.jsp").forward(request, response);
                } else {
                    response.sendRedirect(successURL + "?sloSupported=" + responseTO.isSloSupported());
                }
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
                        + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            }
        }
    }
}
