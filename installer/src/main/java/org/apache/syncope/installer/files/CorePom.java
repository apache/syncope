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

public final class CorePom {

    public static final String SWAGGER_PLACEHOLDER = "</dependencies>";

    public static final String SWAGGER_CONTENT_TO_ADD = "  <dependency>\n"
            + "      <groupId>org.apache.syncope.ext</groupId>\n"
            + "      <artifactId>syncope-ext-swagger-ui</artifactId>\n"
            + "      <version>${syncope.version}</version>\n"
            + "    </dependency>\n"
            + "  </dependencies>\n";

    public static final String ACTIVITI_PLACEHOLDER = "    <dependency>\n"
            + "      <groupId>org.apache.syncope.core</groupId>\n"
            + "      <artifactId>syncope-core-workflow-activiti</artifactId>\n"
            + "    </dependency>\n";

    private CorePom() {
        // private constructor for static utility class
    }
}
