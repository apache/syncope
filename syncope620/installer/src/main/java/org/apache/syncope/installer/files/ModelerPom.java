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

public class ModelerPom {

    public static final String PATH = "/pom.xml";

    public static final String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n"
            + "                             http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "  <modelVersion>4.0.0</modelVersion>\n" + "   \n" + "  <groupId>org.apache.syncope</groupId>\n"
            + "  <artifactId>activitiModelerSetup</artifactId>\n" + "  <version>1.0-SNAPSHOT</version>\n"
            + "  <packaging>jar</packaging>\n" + "   \n" + "  <properties>\n"
            + "    <activiti.version>5.16.2</activiti.version>\n"
            + "    <activiti-modeler.directory>%s</activiti-modeler.directory>\n"
            + "    <tokenValueMap>%s/oryx.debug.js-tokenValueMap.properties</tokenValueMap>\n" + "     \n"
            + "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" + "  </properties>\n" + "   \n"
            + "  <dependencies>\n" + "    <dependency>\n" + "      <groupId>org.activiti</groupId>\n"
            + "      <artifactId>activiti-webapp-explorer2</artifactId>           \n"
            + "      <version>${activiti.version}</version>\n" + "      <type>war</type>\n"
            + "      <scope>test</scope>\n" + "    </dependency>   \n" + "  </dependencies>\n" + "   \n"
            + "  <build>   \n" + "    <plugins>\n" + "      <plugin>\n"
            + "        <groupId>org.apache.maven.plugins</groupId>\n"
            + "        <artifactId>maven-antrun-plugin</artifactId>\n" + "        <version>1.7</version>\n"
            + "        <executions>\n" + "          <execution>\n" + "            <id>setupActivitiModeler</id>\n"
            + "            <phase>process-resources</phase>\n" + "            <configuration>\n"
            + "              <target>\n"
            + "                <unzip src=\"${settings.localRepository}/org/activiti/activiti-webapp-explorer2/${activiti.version}/activiti-webapp-explorer2-${activiti.version}.war\"\n"
            + "                       dest=\"${project.build.directory}/activiti-webapp-explorer2\" />\n"
            + "                 \n" + "                <mkdir dir=\"${activiti-modeler.directory}\" />\n"
            + "                 \n" + "                <mkdir dir=\"${activiti-modeler.directory}/editor\" />\n"
            + "                <copy todir=\"${activiti-modeler.directory}/editor\">\n"
            + "                  <fileset dir=\"${project.build.directory}/activiti-webapp-explorer2/editor\">\n"
            + "                    <exclude name=\"oryx.js\" />\n" + "                  </fileset>\n"
            + "                </copy>\n"
            + "                <copy file=\"${project.build.directory}/activiti-webapp-explorer2/WEB-INF/classes/plugins.xml\"\n"
            + "                      todir=\"${activiti-modeler.directory}/editor\" />\n"
            + "                <copy file=\"${project.build.directory}/activiti-webapp-explorer2/WEB-INF/classes/stencilset.json\"\n"
            + "                      todir=\"${activiti-modeler.directory}/editor\" />\n" + "                 \n"
            + "                <mkdir dir=\"${activiti-modeler.directory}/explorer\" />\n"
            + "                <copy todir=\"${activiti-modeler.directory}/explorer\">\n"
            + "                  <fileset dir=\"${project.build.directory}/activiti-webapp-explorer2/explorer\" />\n"
            + "                </copy>\n" + "                 \n"
            + "                <mkdir dir=\"${activiti-modeler.directory}/libs\" />\n"
            + "                <copy todir=\"${activiti-modeler.directory}/libs\">\n"
            + "                  <fileset dir=\"${project.build.directory}/activiti-webapp-explorer2/libs\" />\n"
            + "                </copy>\n" + "              </target>\n" + "            </configuration>\n"
            + "            <goals>\n" + "              <goal>run</goal>\n" + "            </goals>\n"
            + "          </execution>\n" + "        </executions>\n" + "      </plugin>\n" + "      <plugin>\n"
            + "        <groupId>com.google.code.maven-replacer-plugin</groupId>\n"
            + "        <artifactId>replacer</artifactId>\n" + "        <version>1.5.3</version>\n"
            + "        <executions>\n" + "          <execution>\n" + "            <phase>process-resources</phase>\n"
            + "            <goals>\n" + "              <goal>replace</goal>\n"
            + "            </goals>                  \n" + "          </execution>\n" + "        </executions>\n"
            + "        <configuration>\n"
            + "          <file>${activiti-modeler.directory}/editor/oryx.debug.js</file>\n"
            + "          <tokenValueMap>${tokenValueMap}</tokenValueMap>\n" + "          <unescape>true</unescape>\n"
            + "          <regex>false</regex>\n" + "        </configuration>\n" + "      </plugin>\n"
            + "    </plugins>\n" + "  </build>\n" + "</project>";

}
