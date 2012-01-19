<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@page import="org.syncope.client.SyncopeConstants"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="org.apache.openjpa.datacache.CacheStatisticsImpl"%>
<%@page import="javax.persistence.EntityManagerFactory"%>
<%@page import="org.apache.openjpa.persistence.OpenJPAPersistence"%>
<%@page import="org.apache.openjpa.persistence.OpenJPAEntityManagerFactory"%>
<%@page import="org.syncope.core.util.ApplicationContextManager"%>
<%@page import="org.springframework.context.ConfigurableApplicationContext"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Cache Statistics</title>
        <style type="text/css">
            .c{
                text-align: center;
            }
            .r{
                text-align: right;
            }
            .l{
                text-align: left;
            }
            .t{
                vertical-align: top;
            }
            .b{
                vertical-align: bottom;
            }
            .odd{
                background-color: #D4D4D4;
            }
            .even{
                background-color: #EEEEEE;
            }
            .bd1{
                border: solid #888888 1px;
            }
            .bg1{
                background-color: #CCCCCC;
            }
            .bg2{
                background-color: #DDDDDD;
            }
        </style>
    </head>
    <body>
        <p/>
        <%
            ConfigurableApplicationContext context =
                    ApplicationContextManager.getApplicationContext();

            EntityManagerFactory emf = context.getBean(
                    EntityManagerFactory.class);
            OpenJPAEntityManagerFactory oemf = OpenJPAPersistence.cast(emf);
            CacheStatisticsImpl statistics = (CacheStatisticsImpl) oemf.
                    getStoreCache().getStatistics();

            String action = request.getParameter("do");
            StringBuilder info = new StringBuilder(512);

            if ("activate".equals(action)
                    && !statistics.isEnabled()) {

                statistics.enable();
                info.append("Statistics enabled\n");
            } else if ("deactivate".equals(action)
                    && !statistics.isEnabled()) {

                statistics.disable();
                info.append("Statistics disabled\n");
            } else if ("clear".equals(action)) {
                statistics.reset();
                info.append("Statistics cleared\n");
            }

            SimpleDateFormat sdf = new SimpleDateFormat(
                    SyncopeConstants.DEFAULT_DATE_PATTERN);
            if (info.length() > 0) {
        %>
        <p/><div class="success">
            <c:out value="${fn:escapeXml(info)}"/>
        </div>
        <%                    }%>
        <p/>
        <a href="?">Reload</a>
        <p/>
           <a href="?do=<%=(statistics.isEnabled()
                   ? "deactivate" : "activate")%>">
            <%=(statistics.isEnabled()
                    ? "DEACTIVATE" : "ACTIVATE")%></a>
        <a href="?do=clear">CLEAR</a>
        <p/>
        Last update: <%=sdf.format(statistics.since())%><br/>
        Activation: <%=sdf.format(statistics.start())%><br/>
        <p/>
        <table>
            <tr>
                <th class="c bd1 bg1">Hits</th>
                <td><%=statistics.getHitCount()%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Reads</th>
                <td><%=statistics.getHitCount()%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Writes</th>
                <td><%=statistics.getWriteCount()%></td>
            </tr>
        </table>
        <p/>
        <table width="100%">
            <tr><th colspan="4" class="c bd1 bg2">2nd level cache statistics</th></tr>
            <tr>
                <th class="c bd1 bg1">Region</th>
                <th class="c bd1 bg1">Hits</th>
                <th class="c bd1 bg1">Reads</th>
                <th class="c bd1 bg1">Writes</th>
            </tr>
            <%
                boolean odd = true;
                for (String className : statistics.classNames()) {
            %>
            <tr class="<%=(odd ? "odd" : "even")%>">
                <td><%=className%></td>
                <td><%=statistics.getHitCount(className)%></td>
                <td><%=statistics.getHitCount(className)%></td>
                <td><%=statistics.getWriteCount(className)%></td>
            </tr>
            <%
                    odd = !odd;
                }
            %>
        </table>
    </body>
</html>