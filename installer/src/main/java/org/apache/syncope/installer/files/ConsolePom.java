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

public final class ConsolePom {

    public static final String ACTIVITI_PLACEHOLDER = "</dependencies>";

    public static final String ACTIVITI_CONTENT_TO_ADD = "<dependency>\n"
            + "          <groupId>org.activiti</groupId>\n"
            + "          <artifactId>activiti-webapp-explorer2</artifactId>\n"
            + "          <type>war</type>\n"
            + "          <scope>test</scope>\n"
            + "        </dependency>\n"
            + "  </dependencies>\n";

    public static final String MODELER_PLACEHOLDER = "</finalName>";

    public static final String MODELER_CONTENT_TO_ADD = "</finalName><plugins>\n"
            + "          <plugin>\n"
            + "            <groupId>org.apache.maven.plugins</groupId>\n"
            + "            <artifactId>maven-antrun-plugin</artifactId>\n"
            + "            <inherited>true</inherited>\n"
            + "            <executions>\n"
            + "              <execution>\n"
            + "                <id>setupActivitiModeler</id>\n"
            + "                <phase>process-resources</phase>\n"
            + "                <configuration>\n"
            + "                  <target>\n"
            + "                    <unzip src=\"${settings.localRepository}/org/activiti/activiti-webapp-explorer2/"
            + "${activiti.version}/activiti-webapp-explorer2-${activiti.version}.war\" \n"
            + "                           dest=\"${project.build.directory}/activiti-webapp-explorer2\" />\n"
            + "                \n"
            + "                    <mkdir dir=\"${activiti-modeler.directory}\" />\n"
            + "                    <copy file=\"${project.build.directory}/activiti-webapp-explorer2/modeler.html\" \n"
            + "                          todir=\"${activiti-modeler.directory}\" />\n"
            + "                    <replace file=\"${activiti-modeler.directory}/modeler.html\"\n"
            + "                             token=\"&lt;/head&gt;\"\n"
            + "                             value=\"&lt;script type=&quot;text/javascript&quot;&gt;window.onunload = "
            + "refreshParent; function refreshParent() { window.opener.location.reload(); }&lt;/script&gt;&lt;/head&gt;"
            + "\"/>\n"
            + "                    <copy file=\"${project.build.directory}/activiti-webapp-explorer2/WEB-INF/classes/"
            + "stencilset.json\" \n"
            + "                          todir=\"${activiti-modeler.directory}\" />\n" + "\n"
            + "                    <mkdir dir=\"${activiti-modeler.directory}/editor-app\" />\n"
            + "                    <copy todir=\"${activiti-modeler.directory}/editor-app\">\n"
            + "                      <fileset dir=\"${project.build.directory}/activiti-webapp-explorer2/editor-app\""
            + "/>\n"
            + "                    </copy>\n"
            + "                    <replaceregexp file=\"${activiti-modeler.directory}/editor-app/editor/oryx.debug.js"
            + "\"\n"
            + "                                   match=\"ORYX.CONFIG.ROOT_PATH =.*&quot;editor/&quot;; //TODO:"
            + " Remove last slash!!\"\n"
            + "                                   replace=\"BASE_PATH = window.location.toString().substr(0, "
            + "window.location.toString().indexOf(&#39;/wicket&#39;));\n"
            + "ORYX.CONFIG.ROOT_PATH = BASE_PATH + &quot;/activiti-modeler/editor-app/editor/&quot;;\"\n"
            + "                                   byline=\"true\"/>\n"
            + "                    <replace file=\"${activiti-modeler.directory}/editor-app/editor/oryx.debug.js\"\n"
            + "                             token=\"new Ajax.Request(ACTIVITI.CONFIG.contextRoot + &#39;/editor/"
            + "stencilset?version=&#39; + Date.now(), {\"\n"
            + "                             value=\"new Ajax.Request(window.location.toString().substr(0,"
            + " window.location.toString().indexOf(&#39;/activiti-modeler&#39;)) + &quot;/activiti-modeler/"
            + "stencilset.json&quot;, {\"/>\n"
            + "                    <replace file=\"${activiti-modeler.directory}/editor-app/editor/oryx.debug.js\"\n"
            + "                             token=\"ORYX.Editor.createByUrl(modelUrl);\"\n"
            + "                             value=\"modelUrl = BASE_PATH + &quot;/workflowDefGET?modelId=&quot; "
            + "+ this.modelId; ORYX.Editor.createByUrl(modelUrl);\" />\n"
            + "                    <replace file=\"${activiti-modeler.directory}/editor-app/configuration/"
            + "toolbar-default-actions.js\"\n"
            + "                             token=\"window.location.href = &quot;./&quot;;\"\n"
            + "                             value=\"window.close();\"/>\n"
            + "                                               \n"
            + "                    <copy file=\"${basedir}/src/main/resources/url-config.js\" \n"
            + "                          todir=\"${activiti-modeler.directory}/editor-app/configuration\"\n"
            + "                          overwrite=\"true\" />\n"
            + "                    <copy file=\"${basedir}/src/main/resources/save-model.html\" \n"
            + "                          todir=\"${activiti-modeler.directory}/editor-app/popups\"\n"
            + "                          overwrite=\"true\" />\n"
            + "                  </target>\n"
            + "                </configuration>\n"
            + "                <goals>\n"
            + "                  <goal>run</goal>\n"
            + "                </goals>\n"
            + "              </execution>\n"
            + "            </executions>\n"
            + "          </plugin>\n"
            + "        </plugins>";

    private ConsolePom() {
        // private constructor for static utility class
    }
}
