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

public class CoreWebXml {

    public static final String PLACEHOLDER = "</web-app>";

    private static final String WITH_DATA_SOURCE = "<!-- Uncomment this when using JNDI DataSource -->\n"
            + "  <resource-ref>\n"
            + "    <res-ref-name>jdbc/syncopeDataSource</res-ref-name>\n"
            + "    <res-type>javax.sql.DataSource</res-type>\n"
            + "    <res-auth>Container</res-auth>\n"
            + "  </resource-ref>\n"
            + "</web-app>";

    private static final String WITH_DATA_SOURCE_FOR_JBOSS = "<!-- Uncomment this when using JNDI DataSource -->\n"
            + "  <resource-ref>\n"
            + "    <res-ref-name>jdbc/syncopeDataSource</res-ref-name>\n"
            + "    <res-type>javax.sql.DataSource</res-type>\n"
            + "    <res-auth>Container</res-auth>\n"
            + "    <lookup-name>java:/syncopeDataSource</lookup-name>\n"
            + "  </resource-ref>\n"
            + "</web-app>";

    public static String withDataSource(final String content) {
        return content.replace(PLACEHOLDER, WITH_DATA_SOURCE);
    }

    public static String withDataSourceForJBoss(final String content) {
        return content.replace(PLACEHOLDER, WITH_DATA_SOURCE_FOR_JBOSS);
    }
}
