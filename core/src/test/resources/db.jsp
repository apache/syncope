<%@page import="java.sql.SQLException"%>
<%@page import="org.h2.tools.Server"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    Server h2Datastore = (Server) getServletContext().getAttribute(
            "H2_DATASTORE");
    if (h2Datastore == null || !h2Datastore.isRunning(true)) {
        try {
            h2Datastore = Server.createWebServer("-webPort", "8082");
            h2Datastore.start();

            getServletContext().setAttribute("H2_DATASTORE", h2Datastore);
        } catch (SQLException e) {
            log("Could not start H2 web console (datastore)", e);
        }

        response.sendRedirect("http://localhost:8082");
    }
%>