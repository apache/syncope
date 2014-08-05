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

import org.apache.syncope.installer.utilities.FileSystemUtils;
import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.containers.Glassfish;
import org.apache.syncope.installer.containers.Tomcat;
import org.apache.syncope.installer.containers.jboss.JBoss;
import org.apache.syncope.installer.enums.Containers;
import org.apache.syncope.installer.files.GlassfishCoreWebXml;
import org.apache.syncope.installer.files.JBossDeploymentStructureXml;
import org.apache.syncope.installer.files.PersistenceContextEMFactoryXml;
import org.apache.syncope.installer.files.CoreWebXml;
import org.apache.syncope.installer.utilities.MavenUtils;

public class ContainerProcess {

    private String installPath;

    private String mavenDir;

    private String artifactId;

    private String tomcatUser;

    private String tomcatPassword;

    private boolean tomcatSsl;

    private String tomcatHost;

    private String tomcatPort;

    private String glassfishDir;

    private String confDirectory;

    private String logsDirectory;

    private String bundlesDirectory;

    private boolean withDataSource;

    private boolean jbossSsl;

    private String jbossHost;

    private String jbossPort;

    private String jbossJdbcModuleName;

    private String jbossAdminUsername;

    private String jbossAdminPassword;

    public void run(final AbstractUIProcessHandler handler, final String[] args) {

        installPath = args[0];
        mavenDir = args[1];
        artifactId = args[2];
        final Containers selectedContainer = Containers.fromContainerName(args[3]);
        tomcatSsl = Boolean.valueOf(args[4]);
        tomcatHost = args[5];
        tomcatPort = args[6];
        tomcatUser = args[7];
        tomcatPassword = args[8];
        glassfishDir = args[9];
        confDirectory = args[10];
        logsDirectory = args[11];
        bundlesDirectory = args[12];
        withDataSource = Boolean.valueOf(args[13]);
        jbossSsl = Boolean.valueOf(args[14]);
        jbossHost = args[15];
        jbossPort = args[16];
        jbossJdbcModuleName = args[17];
        jbossAdminUsername = args[18];
        jbossAdminPassword = args[19];

        if (withDataSource) {
            FileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + CoreWebXml.PATH), CoreWebXml.withDataSource());
            switch (selectedContainer) {
                case JBOSS:
                    FileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + CoreWebXml.PATH),
                            CoreWebXml.withDataSourceForJBoss());
                    FileSystemUtils.writeToFile(new File(installPath + "/" + artifactId
                            + PersistenceContextEMFactoryXml.PATH), PersistenceContextEMFactoryXml.FILE);
                    FileSystemUtils.writeToFile(new File(installPath + "/" + artifactId
                            + JBossDeploymentStructureXml.PATH),
                            String.format(JBossDeploymentStructureXml.FILE, jbossJdbcModuleName));
                    break;
                case GLASSFISH:
                    FileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + GlassfishCoreWebXml.PATH),
                            GlassfishCoreWebXml.withDataSource());
                    break;
            }
        }

        final MavenUtils mavenUtils = new MavenUtils(mavenDir);
        mavenUtils.createPackage(installPath + "/" + artifactId, confDirectory, logsDirectory, bundlesDirectory);

        switch (selectedContainer) {
            case TOMCAT:
                final Tomcat tomcat = new Tomcat(
                        tomcatSsl, tomcatHost, tomcatPort, installPath, artifactId, tomcatUser, tomcatPassword);
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
                        jbossSsl, jbossHost, jbossPort, jbossAdminUsername, jbossAdminPassword, installPath, artifactId);

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
                FileSystemUtils.exec(createJavaOptCommand, handler, null);

                final Glassfish glassfish = new Glassfish(installPath, artifactId);

                FileSystemUtils.exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND + glassfish.deployCore(), handler,
                        null);
                FileSystemUtils.exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND + glassfish.deployConsole(),
                        handler, null);
                break;
        }
    }

}
