<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="
        org.syncope.identityconnectors.bundles.staticwebservice.wstarget.DefaultContentLoader,
        java.sql.*,
        org.springframework.jdbc.datasource.DataSourceUtils" %>
<%!
    private void logTableContent(Connection conn, String tableName,
            JspWriter out) throws Exception {

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();

            rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData metaData = rs.getMetaData();
            out.println("<a name=\"" + tableName + "\"><strong>Table: "
                    + tableName + "</strong></a>");
            out.println("<a href=\"#top\">Back</a>");

            out.println("<table border=\"1\">");
            out.println("<thead>");
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                out.println("<th>" + metaData.getColumnLabel(i + 1) + "</th>");
            }
            out.println("</thead>");
            out.println("<tbody>");

            while (rs.next()) {
                out.println("<tr>");
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    if (rs.getString(i + 1) != null
                            && rs.getString(i + 1).length() > 100) {

                        out.println("<td>DATA</td>");
                    } else {
                        out.println("<td>" + rs.getString(i + 1) + "</td>");
                    }
                }
                out.println("</tr>");
            }
            out.println("</tbody>");
            out.println("</table>");
            out.println("<br/>");
        } catch (Exception e) {
            throw e;
        } finally {
            rs.close();
            stmt.close();
        }
    }
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>DB content</title>
    </head>
    <body>
        <%
                    Connection conn = DataSourceUtils.getConnection(
                            DefaultContentLoader.localDataSource);

        %>
        <h2><a href="?reset=true">DB RESET</a></h2>
        <%
                    if (request.getParameter("reset") != null
                            && "true".equals(request.getParameter("reset"))) {

                        Statement stmt = null;
                        try {
                            stmt = conn.createStatement();
                            stmt.executeUpdate("DELETE FROM user");
                        } catch (Exception e) {
                        } finally {
                            stmt.close();
                        }
                    }

                    logTableContent(conn, "user", out);

                    conn.close();
        %>
    </body>
</html>
