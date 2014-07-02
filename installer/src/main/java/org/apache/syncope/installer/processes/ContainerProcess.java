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
package org.apache.syncope.installer.processes;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.containers.Glassfish;
import org.apache.syncope.installer.containers.Tomcat;
import org.apache.syncope.installer.containers.jboss.JBoss;
import org.apache.syncope.installer.enums.Containers;
import org.apache.syncope.installer.files.GlassfishWebXml;
import org.apache.syncope.installer.files.JBossDeploymentStructureXml;
import org.apache.syncope.installer.files.PersistenceContextEMFactoryXml;
import org.apache.syncope.installer.files.WebXml;
import org.apache.syncope.installer.utilities.Commands;

public class ContainerProcess extends AbstractProcess {

    private String installPath;

    private String artifactId;

    private String tomcatUser;

    private String tomcatPassword;

    private String tomcatHost;

    private String tomcatPort;

    private String glassfishDir;

    private String logsDirectory;

    private String bundlesDirectory;

    private boolean withDataSource;

    private String jbossHost;

    private String jbossPort;

    private String jbossJdbcModuleName;

    private String jbossAdminUsername;

    private String jbossAdminPassword;

    public void run(final AbstractUIProcessHandler handler, final String[] args) {

        installPath = args[0];
        artifactId = args[1];
        final Containers selectedContainer = Containers.fromContainerName(args[2]);
        tomcatHost = args[3];
        tomcatPort = args[4];
        tomcatUser = args[5];
        tomcatPassword = args[6];
        glassfishDir = args[7];
        logsDirectory = args[8];
        bundlesDirectory = args[9];
        withDataSource = Boolean.valueOf(args[10]);
        jbossHost = args[11];
        jbossPort = args[12];
        jbossJdbcModuleName = args[13];
        jbossAdminUsername = args[14];
        jbossAdminPassword = args[15];

        if (withDataSource) {
            writeToFile(new File(installPath + "/" + artifactId + WebXml.PATH), WebXml.withDataSource());
            switch (selectedContainer) {
                case JBOSS:
                    writeToFile(new File(installPath + "/" + artifactId + WebXml.PATH), WebXml.withDataSourceForJBoss());
                    writeToFile(new File(installPath + "/" + artifactId + PersistenceContextEMFactoryXml.PATH),
                            PersistenceContextEMFactoryXml.FILE);
                    writeToFile(new File(installPath + "/" + artifactId + JBossDeploymentStructureXml.PATH),
                            String.format(JBossDeploymentStructureXml.FILE, jbossJdbcModuleName));
                    break;
                case GLASSFISH:
                    writeToFile(new File(installPath + "/" + artifactId + GlassfishWebXml.PATH),
                            GlassfishWebXml.withDataSource());
                    break;
            }
        }

        exec(String.format(
                Commands.compileCommand, logsDirectory, bundlesDirectory), handler, installPath + "/" + artifactId);

        switch (selectedContainer) {
            case TOMCAT:
                final Tomcat tomcat = new Tomcat(
                        tomcatHost, tomcatPort, installPath, artifactId, tomcatUser, tomcatPassword);
                boolean deployCoreResult = tomcat.deployCore();
                if (deployCoreResult) {
                    handler.logOutput("Core successfully deployed ", true);
                } else {
                    handler.emitError("Deploy core on Tomcat failed", "Deploy core on Tomcat failed");
                }

                boolean deployConsoleResult = tomcat.deployConsole();
                if (deployConsoleResult) {
                    handler.logOutput("Console successfully deployed ", true);
                } else {
                    handler.emitError("Deploy console on Tomcat failed", "Deploy console on Tomcat failed");
                }
                break;
            case JBOSS:
                final JBoss jBoss = new JBoss(
                        jbossHost, jbossPort, jbossAdminUsername, jbossAdminPassword, installPath, artifactId);

                boolean deployCoreJboss = jBoss.deployCore();
                if (deployCoreJboss) {
                    handler.logOutput("Core successfully deployed ", true);
                } else {
                    handler.emitError("Deploy core on JBoss failed", "Deploy core on JBoss failed");
                }

                boolean deployConsoleJBoss = jBoss.deployConsole();
                if (deployConsoleJBoss) {
                    handler.logOutput("Console successfully deployed ", true);
                } else {
                    handler.emitError("Deploy console on JBoss failed", "Deploy console on JBoss failed");
                }
                break;
            case GLASSFISH:
                final String createJavaOptCommand = "sh " + glassfishDir + Glassfish.CREATE_JAVA_OPT_COMMAND;
                exec(createJavaOptCommand, handler, null);
                exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND
                        + String.format(Glassfish.deploySyncopeCore, installPath, artifactId), handler, null);
                exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND
                        + String.format(Glassfish.deploySyncopeConsole, installPath, artifactId), handler, null);
                break;
        }
    }

}
