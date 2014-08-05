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

public class Pom {

    public static final String PATH = "/pom.xml";

    public static final String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!--\n" +
"Licensed to the Apache Software Foundation (ASF) under one\n" +
"or more contributor license agreements.  See the NOTICE file\n" +
"distributed with this work for additional information\n" +
"regarding copyright ownership.  The ASF licenses this file\n" +
"to you under the Apache License, Version 2.0 (the\n" +
"\"License\"); you may not use this file except in compliance\n" +
"with the License.  You may obtain a copy of the License at\n" +
"\n" +
"  http://www.apache.org/licenses/LICENSE-2.0\n" +
"\n" +
"Unless required by applicable law or agreed to in writing,\n" +
"software distributed under the License is distributed on an\n" +
"\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
"KIND, either express or implied.  See the License for the\n" +
"specific language governing permissions and limitations\n" +
"under the License.\n" +
"\n" +
"--><project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0          http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
"\n" +
"  <modelVersion>4.0.0</modelVersion>\n" +
"\n" +
"  <parent>\n" +
"    <groupId>org.apache.syncope</groupId>\n" +
"    <artifactId>syncope</artifactId>\n" +
"    <version xmlns=\"\">%s</version>\n" +
"  </parent>\n" +
"\n" +
"  <properties>\n" +
"    <syncope.version xmlns=\"\">%s</syncope.version>\n" +
"    <secretKey>ahsdgausygdausygduasygd</secretKey>\n" +
"    <anonymousKey>asdasdasdasd</anonymousKey>\n" +
"  </properties>\n" +
"\n" +
"  <name>Apache Syncope sample project</name>\n" +
"  <groupId>%s</groupId>\n" +
"  <artifactId>%s</artifactId>\n" +
"  <version>1.0-SNAPSHOT</version>\n" +
"  <packaging>pom</packaging>\n" +
"\n" +
"  <dependencyManagement>\n" +
"    <dependencies>\n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-common</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"      </dependency> \n" +
" \n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-common</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"        <classifier>javadoc</classifier>\n" +
"      </dependency>\n" +
"\n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-client</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"      </dependency> \n" +
"\n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-build-tools</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"        <type>war</type>\n" +
"        <scope>test</scope>\n" +
"      </dependency> \n" +
"            \n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-core</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"        <type>war</type>\n" +
"      </dependency> \n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-core</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"        <classifier>classes</classifier>\n" +
"        <scope>provided</scope>\n" +
"      </dependency> \n" +
"\n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-console</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"        <type>war</type>\n" +
"      </dependency> \n" +
"      <dependency>\n" +
"        <groupId>org.apache.syncope</groupId>\n" +
"        <artifactId>syncope-console</artifactId>\n" +
"        <version>${syncope.version}</version>\n" +
"        <classifier>classes</classifier>\n" +
"        <scope>provided</scope>\n" +
"      </dependency> \n" +
"    </dependencies>\n" +
"  </dependencyManagement>\n" +
"    \n" +
"  <build>\n" +
"    \n" +
"    <pluginManagement>\n" +
"      <plugins>\n" +
"        <!-- Disable LICENSE / NOTICE inclusion: see SYNCOPE-84 -->\n" +
"        <plugin>\n" +
"          <groupId>org.apache.maven.plugins</groupId>\n" +
"          <artifactId>maven-war-plugin</artifactId>\n" +
"          <inherited>false</inherited>\n" +
"          <configuration>\n" +
"            <webResources>\n" +
"              <resource>\n" +
"                <directory>src/main/webapp</directory>\n" +
"                <includes>\n" +
"                  <include>**/*.jsp</include>\n" +
"                </includes>\n" +
"                <filtering>true</filtering>\n" +
"              </resource>\n" +
"            </webResources>\n" +
"          </configuration>\n" +
"        </plugin>\n" +
"      </plugins>\n" +
"    </pluginManagement>\n" +
"    \n" +
"    <!-- Disable legal check for generated projects: see SYNCOPE-84 -->\n" +
"    <plugins>\n" +
"      <plugin>\n" +
"        <groupId>org.codehaus.mojo</groupId>\n" +
"        <artifactId>ianal-maven-plugin</artifactId>\n" +
"        <inherited>true</inherited>\n" +
"        <executions>\n" +
"          <execution>            \n" +
"            <goals>\n" +
"              <goal>verify-legal-files</goal>\n" +
"            </goals>\n" +
"            <phase>none</phase>\n" +
"          </execution>\n" +
"        </executions>\n" +
"      </plugin>\n" +
"      <plugin>\n" +
"        <groupId>org.apache.rat</groupId>\n" +
"        <artifactId>apache-rat-plugin</artifactId>\n" +
"        <inherited>true</inherited>\n" +
"        <executions>\n" +
"          <execution>\n" +
"            <goals>\n" +
"              <goal>check</goal>\n" +
"            </goals>\n" +
"            <phase>none</phase>\n" +
"          </execution>\n" +
"        </executions>\n" +
"      </plugin>\n" +
"    </plugins>\n" +
"  </build>\n" +
"<repositories>\n" +
"  <repository>\n" +
"    <id>ASF</id>\n" +
"    <url>https://repository.apache.org/content/repositories/snapshots/</url>\n" +
"    <snapshots>\n" +
"      <enabled>true</enabled>\n" +
"    </snapshots>\n" +
"  </repository>\n" +
"</repositories>\n" +
"    \n" +
"  <modules>\n" +
"    <module>core</module>\n" +
"    <module>console</module>\n" +
"  </modules>\n" +
"\n" +
"</project>";

}
