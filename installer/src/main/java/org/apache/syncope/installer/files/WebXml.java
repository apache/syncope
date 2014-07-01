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
package org.apache.syncope.installer.files;

public class WebXml {

    public static final String PATH = "/core/src/main/webapp/WEB-INF/web.xml";

    public static String withDataSource() {
        return String.format(FILE, withDataSource);
    }

    public static String withDataSourceForJBoss() {
        return String.format(FILE, withDataSourceForJBoss);
    }

    private static final String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!--\n"
            + "Licensed to the Apache Software Foundation (ASF) under one\n"
            + "or more contributor license agreements.  See the NOTICE file\n"
            + "distributed with this work for additional information\n"
            + "regarding copyright ownership.  The ASF licenses this file\n"
            + "to you under the Apache License, Version 2.0 (the\n"
            + "\"License\"); you may not use this file except in compliance\n"
            + "with the License.  You may obtain a copy of the License at\n" + "\n"
            + "  http://www.apache.org/licenses/LICENSE-2.0\n" + "\n"
            + "Unless required by applicable law or agreed to in writing,\n"
            + "software distributed under the License is distributed on an\n"
            + "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n"
            + "KIND, either express or implied.  See the License for the\n"
            + "specific language governing permissions and limitations\n" + "under the License.\n" + "-->\n"
            + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee \n"
            + "                             http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         version=\"3.0\">\n" + "\n" + "  <display-name>Apache Syncope core</display-name>\n" + "\n"
            + "  <context-param>\n" + "    <param-name>contextConfigLocation</param-name>\n"
            + "    <param-value>classpath*:/*Context.xml</param-value>\n" + "  </context-param>\n" + "\n"
            + "  <listener>\n"
            + "    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>\n"
            + "  </listener>\n" + "  <listener>\n"
            + "    <listener-class>org.apache.syncope.core.init.ThreadLocalCleanupListener</listener-class>\n"
            + "  </listener>\n" + "    \n" + "  <servlet>\n" + "    <servlet-name>CXFServlet</servlet-name>\n"
            + "    <servlet-class>org.apache.cxf.transport.servlet.CXFServlet</servlet-class>\n"
            + "    <load-on-startup>1</load-on-startup> \n" + "  </servlet>\n" + "  <servlet-mapping>\n"
            + "    <servlet-name>CXFServlet</servlet-name>\n" + "    <url-pattern>/rest/*</url-pattern>\n"
            + "  </servlet-mapping>\n" + "  <servlet>\n" + "    <servlet-name>WADLServlet</servlet-name>\n"
            + "    <servlet-class>org.apache.syncope.core.rest.WADLServlet</servlet-class>\n"
            + "    <load-on-startup>2</load-on-startup> \n" + "  </servlet>\n" + "  <servlet-mapping>\n"
            + "    <servlet-name>WADLServlet</servlet-name>\n" + "    <url-pattern>/rest/doc/*</url-pattern>\n"
            + "  </servlet-mapping>\n" + "\n" + "  <filter>\n" + "    <filter-name>encodingFilter</filter-name>\n"
            + "    <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>\n"
            + "    <init-param>\n" + "      <param-name>encoding</param-name>\n"
            + "      <param-value>UTF-8</param-value>\n" + "    </init-param>\n" + "    <init-param>\n"
            + "      <param-name>forceEncoding</param-name>\n" + "      <param-value>true</param-value>\n"
            + "    </init-param>\n" + "  </filter>\n" + "  <filter>\n"
            + "    <filter-name>springSecurityFilterChain</filter-name>\n"
            + "    <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>\n"
            + "  </filter>\n" + "\n" + "  <filter-mapping>\n" + "    <filter-name>encodingFilter</filter-name>\n"
            + "    <url-pattern>/*</url-pattern>\n" + "  </filter-mapping>\n" + "  <filter-mapping>\n"
            + "    <filter-name>springSecurityFilterChain</filter-name>\n" + "    <url-pattern>/*</url-pattern>\n"
            + "  </filter-mapping>\n" + "\n" + "  <login-config>\n" + "    <auth-method>CLIENT-CERT</auth-method>\n"
            + "  </login-config>\n" + "    \n" + "  <!-- Uncomment this when using JNDI DataSource -->\n"
            + "%s"
            + "</web-app>\n";

    private static final String withDataSource = "<!-- Uncomment this when using JNDI DataSource -->\n"
            + "  <resource-ref>\n"
            + "    <res-ref-name>jdbc/syncopeDataSource</res-ref-name>\n"
            + "    <res-type>javax.sql.DataSource</res-type>\n"
            + "    <res-auth>Container</res-auth>\n"
            + "  </resource-ref>\n" + "\n";

    private static final String withDataSourceForJBoss = "<!-- Uncomment this when using JNDI DataSource -->\n"
            + "  <resource-ref>\n"
            + "    <res-ref-name>jdbc/syncopeDataSource</res-ref-name>\n"
            + "    <res-type>javax.sql.DataSource</res-type>\n"
            + "    <res-auth>Container</res-auth>\n"
            + "    <lookup-name>java:/syncopeDataSource</lookup-name>\n"
            + "  </resource-ref>\n" + "\n";

}
