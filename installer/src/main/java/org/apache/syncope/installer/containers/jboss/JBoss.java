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
package org.apache.syncope.installer.containers.jboss;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import org.apache.syncope.installer.containers.AbstractContainer;
import org.apache.syncope.installer.utilities.HttpUtils;
import org.apache.syncope.installer.utilities.JsonUtils;

public class JBoss extends AbstractContainer {

    private final String addContentUrl = "http://%s:%s/management/add-content";

    private final String enableUrl = "http://%s:%s/management/";

    private final boolean jbossSsl;

    private final String jbossHost;

    private final String jbossManagementPort;

    private final String installPath;

    private final String artifactId;

    private final HttpUtils httpUtils;

    public JBoss(final boolean jbossSsl, final String jbossHost, final String jbossManagementPort,
            final String jbossAdminUsername, final String jbossAdminPassword,
            final String installPath, final String artifactId, final AbstractUIProcessHandler handler) {

        this.jbossSsl = jbossSsl;
        this.jbossHost = jbossHost;
        this.jbossManagementPort = jbossManagementPort;
        this.installPath = installPath;
        this.artifactId = artifactId;
        this.httpUtils = new HttpUtils(
                jbossSsl, jbossHost, jbossManagementPort, jbossAdminUsername, jbossAdminPassword, handler);

    }

    public boolean deployCore() {
        return deploy(UNIX_CORE_RELATIVE_PATH, "syncope.war");
    }

    public boolean deployConsole() {
        return deploy(UNIX_CONSOLE_RELATIVE_PATH, "syncope-console.war");
    }

    public boolean deployEnduser() {
        return deploy(UNIX_ENDUSER_RELATIVE_PATH, "syncope-console.war");
    }

    public boolean deploy(final String whatDeploy, final String warName) {
        final String responseBodyAsString = httpUtils.postWithDigestAuth(
                String.format(addContentUrl, jbossHost, jbossManagementPort),
                String.format(whatDeploy, installPath, artifactId));

        final JBossAddResponse jBossAddResponse = JsonUtils.jBossAddResponse(responseBodyAsString);

        final JBossDeployRequestContent jBossDeployRequestContent = new JBossDeployRequestContent(
                jBossAddResponse.getResult().getBytesValue(), warName);

        int status = httpUtils.
                postWithStringEntity(String.format(enableUrl, jbossHost, jbossManagementPort),
                        JsonUtils.jBossDeployRequestContent(jBossDeployRequestContent));
        return status == 200;
    }
}
