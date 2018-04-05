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

    public static final String FLOWABLE_PLACEHOLDER = "</dependencies>";

    public static final String FLOWABLE_CONTENT_TO_ADD = "<dependency>\n"
            + "          <groupId>org.flowable</groupId>\n"
            + "          <artifactId>flowable-ui-modeler-app</artifactId>\n"
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
            + "                <id>setupFlowableModeler</id>\n"
            + "                <phase>process-resources</phase>\n"
            + "                <configuration>\n"
            + "                  <target>\n"
            + "                <unzip src=\"${settings.localRepository}/org/flowable/flowable-ui-modeler-app/"
            + "${flowable.version}/flowable-ui-modeler-app-${flowable.version}.war\" \n"
            + "                       dest=\"${flowable-modeler.directory}\">\n" + "                  <patternset>\n"
            + "                    <include name=\"WEB-INF/classes/static/**\"/>\n"
            + "                    <include name=\"WEB-INF/lib/flowable-ui-modeler-logic-${flowable.version}.jar\"/>\n"
            + "                  </patternset>\n" + "                </unzip>\n" + "                \n"
            + "                <unzip src=\"${flowable-modeler.directory}/WEB-INF/lib/"
            + "flowable-ui-modeler-logic-${flowable.version}.jar\"\n"
            + "                       dest=\"${flowable-modeler.directory}\">\n" + "                  <patternset>\n"
            + "                    <include name=\"stencilset_bpmn.json\"/>\n" + "                  </patternset>\n"
            + "                </unzip>\n" + "                \n"
            + "                <move todir=\"${flowable-modeler.directory}\">\n"
            + "                  <fileset dir=\"${flowable-modeler.directory}/WEB-INF/classes/static/\">\n"
            + "                    <include name=\"**\"/>\n" + "                  </fileset>\n"
            + "                </move>\n"
            + "                <delete dir=\"${flowable-modeler.directory}/WEB-INF\"/>\n"
            + "                \n"
            + "                <replace file=\"${flowable-modeler.directory}/index.html\"\n"
            + "                         token=\"&lt;/head&gt;\"\n"
            + "                         value=\"&lt;script type=&quot;text/javascript&quot;&gt;window.onunload = "
            + "refreshParent; function refreshParent() { window.opener.location.reload(); }&lt;/script&gt;&lt;/head&gt;"
            + "\"/>\n"
            + "                <replace file=\"${flowable-modeler.directory}/index.html\"\n"
            + "                         token=\" ng-click=&quot;backToLanding()&quot;\"\n"
            + "                         value=\" disabled=&quot;disabled&quot;\"/>\n"
            + "                <replace file=\"${flowable-modeler.directory}/index.html\"\n"
            + "                         token=\"&lt;ul class=&quot;nav navbar-nav&quot; id=&quot;main-nav&quot;\"\n"
            + "                         value=\"&lt;ul class=&quot;nav navbar-nav&quot; id=&quot;main-nav&quot; "
            + "style=&quot;display: none;&quot;\"/>\n"
            + "                <replace file=\"${flowable-modeler.directory}/index.html\"\n"
            + "                         token=\"&lt;div class=&quot;pull-right\"\n"
            + "                         value=\"&lt;div style=&quot;display: none;&quot; class=&quot;pull-right\"/>\n"
            + "                <replace file=\"${flowable-modeler.directory}/editor-app/editor.html\"\n"
            + "                         token=\"&lt;div class=&quot;btn-group pull-right&quot;\"\n"
            + "                         value=\"&lt;div style=&quot;display: none;&quot; class=&quot;btn-group "
            + "pull-right&quot;\"/>\n"
            + "                <replace file=\"${flowable-modeler.directory}/editor-app/configuration/"
            + "toolbar-default-actions.js\"\n"
            + "                         token=\"$location.path('/processes');\"\n"
            + "                         value=\"window.close();\"/>\n" + " \n"
            + "                <copy file=\"${basedir}/src/main/resources/app-cfg.js\" \n"
            + "                      todir=\"${flowable-modeler.directory}/scripts\"\n"
            + "                      overwrite=\"true\"/>\n"
            + "                <copy file=\"${basedir}/src/main/resources/url-config.js\" \n"
            + "                      todir=\"${flowable-modeler.directory}/editor-app/configuration\"\n"
            + "                      overwrite=\"true\"/>\n"
            + "                <copy file=\"${basedir}/src/main/resources/toolbar.js\" \n"
            + "                      todir=\"${flowable-modeler.directory}/editor-app/configuration\"\n"
            + "                      overwrite=\"true\"/>\n"
            + "                <copy file=\"${basedir}/src/main/resources/save-model.html\" \n"
            + "                      todir=\"${flowable-modeler.directory}/editor-app/popups\"\n"
            + "                      overwrite=\"true\"/>"
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
