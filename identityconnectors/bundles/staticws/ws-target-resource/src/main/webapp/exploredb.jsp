<%-- 
    Document   : exploredb
    Created on : Jul 23, 2010, 5:03:59 PM
    Author     : fabio
--%>

<%@page contentType="text/html" pageEncoding="MacRoman"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="
        org.syncope.identityconnectors.bundles.staticwebservice.wstarget.DefaultContentLoader,
        java.sql.*,
        org.springframework.jdbc.datasource.DataSourceUtils" %>

<%

            Connection conn = DataSourceUtils.getConnection(
                    DefaultContentLoader.localDataSource);

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery("SELECT * FROM user");

%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>JSP Page</title>
    </head>
    <body>
        <%
            ResultSetMetaData metaData = rs.getMetaData();
            StringBuilder row = new StringBuilder();

            while (rs.next()) {
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    row.append(metaData.getColumnLabel(i + 1)).append("=").
                            append(rs.getString(i + 1)).append(" ");
                }

        %>
        <%=row.toString()%><br />;
        <%
                row.delete(0, row.length());
            }

            rs.close();
            st.close();
            conn.close();
        %>
    </body>
</html>
