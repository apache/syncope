<%--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
--%>
<%@page import="java.sql.SQLException"%>
<%@page import="org.h2.tools.Server"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    Server h2Datastore = (Server) getServletConfig().getServletContext().getAttribute("H2_DATASTORE");
    if (h2Datastore == null || !h2Datastore.isRunning(true)) {
        try {
            h2Datastore = Server.createWebServer("-webPort", "8082");
            h2Datastore.start();

            getServletConfig().getServletContext().setAttribute("H2_DATASTORE", h2Datastore);
        } catch (SQLException e) {
            getServletConfig().getServletContext().log("Could not start H2 web console (datastore)", e);
        }
    }
    response.sendRedirect("http://localhost:8082");
%>