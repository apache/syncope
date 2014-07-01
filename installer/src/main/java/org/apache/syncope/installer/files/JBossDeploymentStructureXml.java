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

public class JBossDeploymentStructureXml {

    public static final String PATH = "/core/src/main/webapp/WEB-INF/jboss-deployment-structure.xml";

    public static final String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!--\n"
            + "Licensed to the Apache Software Foundation (ASF) under one\n"
            + "or more contributor license agreements.  See the NOTICE file\n"
            + "distributed with this work for additional information\n"
            + "regarding copyright ownership.  The ASF licenses this file\n"
            + "to you under the Apache License, Version 2.0 (the\n"
            + "\"License\"); you may not use this file except in compliance\n"
            + "with the License.  You may obtain a copy of the License at\n" + "\n"
            + "       http://www.apache.org/licenses/LICENSE-2.0\n" + "\n"
            + "Unless required by applicable law or agreed to in writing,\n"
            + "software distributed under the License is distributed on an\n"
            + "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n"
            + "KIND, either express or implied.  See the License for the\n"
            + "specific language governing permissions and limitations\n" + "under the License.\n" + "\n" + "-->\n"
            + "<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.0\">\n"
            + "  <deployment>\n"
            + "    <dependencies>\n"
            + "      <module name=\"org.apache.xalan\"/>\n"
            + "      <module name=\"%s\"/>\n"
            + "    </dependencies>\n"
            + "    <exclusions>\n"
            + "      <module name=\"org.hibernate\"/>\n"
            + "      <module name=\"org.slf4j\"/>\n"
            + "      <module name=\"org.slf4j.impl\"/>\n"
            + "    </exclusions>\n"
            + "  </deployment>\n"
            + "</jboss-deployment-structure>";

}
