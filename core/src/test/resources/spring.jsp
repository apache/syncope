<%@page import="org.syncope.core.util.ApplicationContextManager"%>
<%@page import="org.springframework.context.ConfigurableApplicationContext"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Spring beans</title>
    </head>
    <body>
        <h1>Spring beans</h1>
        <%
                    ConfigurableApplicationContext context =
                            ApplicationContextManager.getApplicationContext();
        %>
        <h2>Singletons</h2>
        <ul><%
        String[] singletons = context.getBeanFactory().getSingletonNames();
        for (String bean: singletons) {
            %><li><%=bean%></li><%
        }
            %>
        </ul>
        <h2>Bean definitions</h2>
            <ul><%
        String[] prototypes = context.getBeanFactory().getBeanDefinitionNames();
        for (String bean: prototypes) {
            %><li><%=bean%></li><%
        }
            %>
        </ul>
    </body>
</html>
