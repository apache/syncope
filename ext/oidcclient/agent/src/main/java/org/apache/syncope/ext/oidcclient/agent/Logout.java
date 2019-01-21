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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Logout extends HttpServlet {

    private static final long serialVersionUID = 2383239908659843071L;

    protected static final Logger LOG = LoggerFactory.getLogger(Logout.class);

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String successURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGOUT_SUCCESS_URL);
            if (successURL == null) {
                request.getRequestDispatcher("logoutSuccess.jsp").forward(request, response);
            } else {
                response.sendRedirect(successURL);
            }
            request.getSession().removeAttribute(Constants.OIDCCLIENTJWT);
        } catch (Exception e) {
            LOG.error("While processing authentication response from IdP", e);

            String errorURL = getServletContext().getInitParameter(Constants.CONTEXT_PARAM_LOGOUT_ERROR_URL);
            if (errorURL == null) {
                request.setAttribute("exception", e);
                request.getRequestDispatcher("logoutError.jsp").forward(request, response);

                e.printStackTrace(response.getWriter());
            } else {
                response.sendRedirect(errorURL + "?errorMessage="
                        + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            }
        }
    }
}
