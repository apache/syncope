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

public class GlassfishWebXml {

    public static final String PATH = "/core/src/main/webapp/WEB-INF/glassfish-web.xml";

    public static String withDataSource() {
        return String.format(FILE, dataSource);
    }

    private final static String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!--\n"
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
            + "specific language governing permissions and limitations\n" + "under the License.\n" + "\n" + "-->\n"
            + "<!DOCTYPE glassfish-web-app PUBLIC \"-//GlassFish.org//DTD \n"
            + "GlassFish Application Server 3.1 Servlet 3.0//EN\" \"http://glassfish.org/dtds/glassfish-web-app_3_0-1.dtd\">\n"
            + "<glassfish-web-app>\n" + "  <context-root>/syncope</context-root>\n"
            + "%s"
            + "  <class-loader delegate=\"false\"/>\n" + "  <jsp-config>\n"
            + "    <property name=\"httpMethods\" value=\"GET,POST,HEAD,PUT,DELETE\"/>\n" + "  </jsp-config>\n"
            + "</glassfish-web-app>";

    private static final String dataSource = "<!-- Uncomment this when using JNDI DataSource -->\n"
            + "  <resource-ref>\n"
            + "    <res-ref-name>jdbc/syncopeDataSource</res-ref-name>\n"
            + "    <jndi-name>jdbc/syncopeDataSource</jndi-name>\n"
            + "  </resource-ref>\n";

}
