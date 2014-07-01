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

import org.apache.syncope.installer.utilities.HttpUtils;
import org.apache.syncope.installer.utilities.JsonUtils;

public class JBoss {

    private final String core = "%s/%s/core/target/syncope.war";

    private final String console = "%s/%s/console/target/syncope-console.war";

    private final String addContentUrl = "http://%s:%s/management/add-content";

    private final String enableUrl = "http://%s:%s/management/";

    private final String jbossHost;

    private final String jbossPort;

    private final String installPath;

    private final String artifactId;

    private final HttpUtils httpUtils;

    public JBoss(final String jbossHost, final String jbossPort,
            final String jbossAdminUsername, final String jbossAdminPassword,
            final String installPath, final String artifactId) {
        this.jbossHost = jbossHost;
        this.jbossPort = jbossPort;
        this.installPath = installPath;
        this.artifactId = artifactId;
        httpUtils = new HttpUtils(jbossAdminUsername, jbossAdminPassword);

    }

    public boolean deployCore() {
        return deploy(core, "syncope.war");
    }

    public boolean deployConsole() {
        return deploy(console, "syncope-console.war");
    }

    public boolean deploy(final String whatDeploy, final String warName) {
        final String responseBodyAsString = httpUtils.postWithDigestAuth(
                String.format(addContentUrl, jbossHost, jbossPort),
                String.format(whatDeploy, installPath, artifactId));

        final JBossAddResponse jBossAddResponse = JsonUtils.jBossAddResponse(responseBodyAsString);

        final JBossDeployRequestContent jBossDeployRequestContent = new JBossDeployRequestContent(
                jBossAddResponse.getResult().getBYTES_VALUE(), warName);

        int status = httpUtils.postWithStringEntity(String.format(enableUrl, jbossHost, jbossPort),
                JsonUtils.jBossDeployRequestContent(jBossDeployRequestContent));
        return status == 200;
    }
}
