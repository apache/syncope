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
package org.apache.syncope.fit.buildtools;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Just used to verify a connector request timeout.
 */
@WebServlet(urlPatterns = "/services/")
public class ServiceTimeoutServlet extends HttpServlet {

    private static final long serialVersionUID = -1467488672392710293L;

    /**
     * Processes requests for both HTTP
     * {@code GET} and
     * {@code POST} methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    protected static void processRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        try {
            Thread.sleep(60000);
        } catch (InterruptedException ignore) {
            // ignore
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("OK");
        }
    }

    /**
     * Handles the HTTP
     * {@code GET} method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * {@code POST} method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Service Timeout";
    }
}
