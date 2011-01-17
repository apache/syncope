<%@page import="java.sql.ResultSetMetaData"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="java.sql.DatabaseMetaData"%>
<%@page import="org.springframework.jdbc.datasource.DataSourceUtils"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.Connection"%>
<%@page import="org.syncope.core.persistence.util.ApplicationContextManager"%>
<%@page import="org.springframework.context.ConfigurableApplicationContext"%>
<%@page import="org.syncope.core.persistence.DefaultContentLoader"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
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
        <title>Database content</title>
    </head>
    <body>
        <div>
            <%
                        ConfigurableApplicationContext context =
                                ApplicationContextManager.getApplicationContext();
                        DataSource dataSource =
                                (DataSource) context.getBean("localDataSource");

                        Connection conn = DataSourceUtils.getConnection(
                                dataSource);

                        DatabaseMetaData dbm = conn.getMetaData();
                        String[] types = {"TABLE", "VIEW"};
                        ResultSet rs = dbm.getTables(null, null, "%", types);
            %>
            <div style="width:300px;float:left;">
                <a name="top">&#160;</a>
                <ul style="margin-top:0px;">
                    <%
                                while (rs.next()) {
                                    out.println("<li><a href=\"#"
                                            + rs.getString("TABLE_NAME") + "\">"
                                            + rs.getString("TABLE_NAME")
                                            + "</a></li>");
                                }
                    %>
                </ul>
            </div>
            <%
                        rs.beforeFirst();
            %>
            <div style="width:1000px;float:left;">
                <%
                            while (rs.next()) {
                                logTableContent(conn,
                                        rs.getString("TABLE_NAME"), out);
                            }
                %>
            </div>
            <div style="clear:left;">&#160;</div>
            <%
                        rs.close();
                        conn.close();
            %>
        </div>
    </body>
</html>
