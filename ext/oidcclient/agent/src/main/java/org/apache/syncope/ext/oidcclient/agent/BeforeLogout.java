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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.OIDCConstants;
import org.apache.syncope.common.lib.to.OIDCLogoutRequestTO;
import org.apache.syncope.common.rest.api.service.OIDCClientService;
import org.springframework.context.ApplicationContext;

public class BeforeLogout extends AbstractOIDCClientServlet {

    private static final long serialVersionUID = -5920740403138557179L;

    private final boolean useGZIPCompression;

    public BeforeLogout(
            final ApplicationContext ctx,
            final boolean useGZIPCompression) {

        super(ctx);
        this.useGZIPCompression = useGZIPCompression;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);

        String accessToken = (String) request.getSession().getAttribute(Constants.OIDCCLIENTJWT);
        if (StringUtils.isBlank(accessToken)) {
            throw new IllegalArgumentException("No access token found ");
        }

        SyncopeClientFactoryBean clientFactory = getClientFactory(request.getServletContext(), useGZIPCompression);
        OIDCLogoutRequestTO requestTO = clientFactory.create(accessToken).getService(OIDCClientService.class).
                createLogoutRequest(request.getSession().getAttribute(OIDCConstants.OP).toString());

        String postLogoutRedirectURI = StringUtils.substringBefore(request.getRequestURL().toString(), "/beforelogout")
                + "/logout";
        UriBuilder ub = UriBuilder.fromUri(requestTO.getEndSessionEndpoint());
        ub.queryParam(OIDCConstants.POST_LOGOUT_REDIRECT_URI, postLogoutRedirectURI);
        response.setHeader(HttpHeaders.LOCATION, ub.build().toASCIIString());
    }
}
