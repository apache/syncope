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

public final class MasterDomainXml {

    public static final String PLACEHOLDER = ""
            + "<entry key=\"openjpa.MetaDataFactory\" \n               "
            + "value=\"jpa(URLs=vfs:${project.build.directory}/cargo/configurations/wildfly9x/deployments/syncope.war"
            + "/WEB-INF/classes, Resources=${Master.orm})\"/>\n";

    public static final String JBOSS =
            "<entry key=\"openjpa.MetaDataFactory\" value=\"jpa(URLs=vfs:/content/${project.build.finalName}.war/"
            + "WEB-INF/classes/, Resources=${Master.orm})\"/>";

    private MasterDomainXml() {
        // private constructor for static utility class
    }
}
