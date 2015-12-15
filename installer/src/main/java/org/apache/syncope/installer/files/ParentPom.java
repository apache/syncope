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

public final class ParentPom {

    public static final String REPOSITORY_PLACEHOLDER = "</project>";

    public static final String REPOSITORY_CONTENT_TO_ADD = "  <repositories>\n"
            + "    <repository>\n"
            + "      <id>ASF</id>\n"
            + "      <url>https://repository.apache.org/content/repositories/snapshots/</url>\n"
            + "      <snapshots>\n"
            + "        <enabled>true</enabled>\n"
            + "      </snapshots>\n"
            + "    </repository>\n"
            + "  </repositories>\n"
            + "</project>\n";

    private ParentPom() {
        // private constructor for static utility class
    }
}
