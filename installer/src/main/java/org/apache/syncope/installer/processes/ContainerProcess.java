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
import java.io.IOException;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.syncope.installer.containers.Glassfish;
import org.apache.syncope.installer.containers.Tomcat;
import org.apache.syncope.installer.containers.jboss.JBoss;
import org.apache.syncope.installer.enums.Containers;
import org.apache.syncope.installer.files.ConsoleProperties;
import org.apache.syncope.installer.files.EnduserProperties;
import org.apache.syncope.installer.files.GlassfishCoreWebXml;
import org.apache.syncope.installer.files.MasterDomainXml;
import org.apache.syncope.installer.utilities.InstallLog;
import org.apache.syncope.installer.utilities.MavenUtils;
import org.xml.sax.SAXException;

public final class ContainerProcess extends BaseProcess {

    private static final String HTTP = "http";

    private static final String HTTPS = "https";

    private static final String DEFAULT_HOST = "localhost";

    private static final String DEFAULT_PORT = "8080";

    private String installPath;

    private String mavenDir;

    private String artifactId;

    private String tomcatUser;

    private String tomcatPassword;

    private boolean tomcatSsl;

    private String tomcatHost;

    private String tomcatPort;

    private String glassfishDir;

    private boolean glassfishSsl;

    private String glassfishHost;

    private String glassfishPort;

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

    private String jbossManagementPort;

    private boolean isProxyEnabled;

    private String proxyHost;

    private String proxyPort;

    private String proxyUser;

    private String proxyPwd;

    private boolean mavenProxyAutoconf;

    @Override
    public void run(final AbstractUIProcessHandler handler, final String[] args) {
        installPath = args[0];
        mavenDir = args[1];
        artifactId = args[2];
        Containers selectedContainer = Containers.fromContainerName(args[3]);
        tomcatSsl = Boolean.valueOf(args[4]);
        tomcatHost = args[5];
        tomcatPort = args[6];
        tomcatUser = args[7];
        tomcatPassword = args[8];
        glassfishSsl = Boolean.valueOf(args[9]);
        glassfishHost = args[10];
        glassfishPort = args[11];
        glassfishDir = args[12];
        confDirectory = args[13];
        logsDirectory = args[14];
        bundlesDirectory = args[15];
        withDataSource = Boolean.valueOf(args[16]);
        jbossSsl = Boolean.valueOf(args[17]);
        jbossHost = args[18];
        jbossPort = args[19];
        jbossJdbcModuleName = args[20];
        jbossAdminUsername = args[21];
        jbossAdminPassword = args[22];
        jbossManagementPort = args[23];
        isProxyEnabled = Boolean.valueOf(args[24]);
        proxyHost = args[25];
        proxyPort = args[26];
        proxyUser = args[27];
        proxyPwd = args[28];
        mavenProxyAutoconf = Boolean.valueOf(args[29]);

        handler.logOutput("Configure web.xml file according to " + selectedContainer + " properties", true);
        InstallLog.getInstance().info("Configure web.xml file according to " + selectedContainer + " properties");

        FileSystemUtils fileSystemUtils = new FileSystemUtils(handler);
        setSyncopeInstallDir(installPath, artifactId);

        if (withDataSource && selectedContainer == Containers.GLASSFISH) {
            File glassfishCoreWebXmlFile =
                    new File(syncopeInstallDir + PROPERTIES.getProperty("glassfishCoreWebXmlFile"));
            String contentGlassfishWebXmlFile = fileSystemUtils.readFile(glassfishCoreWebXmlFile);
            fileSystemUtils.writeToFile(glassfishCoreWebXmlFile,
                    contentGlassfishWebXmlFile.replace(GlassfishCoreWebXml.PLACEHOLDER,
                            GlassfishCoreWebXml.DATA_SOURCE));
        }

        File consolePropertiesFile = new File(syncopeInstallDir + PROPERTIES.getProperty("consoleResDirectory")
                + File.separator + PROPERTIES.getProperty("consolePropertiesFile"));
        String contentConsolePropertiesFile = fileSystemUtils.readFile(consolePropertiesFile);

        File enduserPropertiesFile = new File(syncopeInstallDir + PROPERTIES.getProperty("enduserResDirectory")
                + File.separator + PROPERTIES.getProperty("enduserPropertiesFile"));
        String contentEnduserPropertiesFile = fileSystemUtils.readFile(enduserPropertiesFile);

        final String scheme;
        final String host;
        final String port;
        switch (selectedContainer) {
            case TOMCAT:
                scheme = tomcatSsl ? HTTPS : HTTP;
                host = tomcatHost;
                port = tomcatPort;
                break;
            case JBOSS:
                scheme = jbossSsl ? HTTPS : HTTP;
                host = jbossHost;
                port = jbossPort;
                persistenceContextEMFactory(fileSystemUtils, handler);
                break;
            case GLASSFISH:
                scheme = glassfishSsl ? HTTPS : HTTP;
                host = glassfishHost;
                port = glassfishPort;
                break;
            default:
                scheme = HTTP;
                host = DEFAULT_HOST;
                port = DEFAULT_PORT;
        }

        fileSystemUtils.writeToFile(consolePropertiesFile,
                contentConsolePropertiesFile.replace(ConsoleProperties.PLACEHOLDER,
                        String.format(ConsoleProperties.CONSOLE, scheme, host, port)));

        fileSystemUtils.writeToFile(enduserPropertiesFile,
                contentEnduserPropertiesFile.replace(EnduserProperties.PLACEHOLDER,
                        String.format(EnduserProperties.ENDUSER, scheme, host, port)));

        MavenUtils mavenUtils = new MavenUtils(mavenDir, handler);
        File customMavenProxySettings = null;
        try {
            if (isProxyEnabled && mavenProxyAutoconf) {
                customMavenProxySettings = MavenUtils.createSettingsWithProxy(
                        installPath, proxyHost, proxyPort, proxyUser, proxyPwd);
            }
        } catch (IOException e) {
            StringBuilder message = new StringBuilder("I/O error during creation of Maven custom settings.xml");
            handler.emitError(message.toString(), e.getMessage());
            InstallLog.getInstance().error(message.append('\n').append(e.getMessage()).toString());
        } catch (ParserConfigurationException e) {
            StringBuilder message = new StringBuilder(
                    "Parser configuration error during creation of Maven custom settings.xml");
            handler.emitError(message.toString(), e.getMessage());
            InstallLog.getInstance().error(message.append('\n').append(e.getMessage()).toString());
        } catch (TransformerException e) {
            StringBuilder message = new StringBuilder(
                    "Transformer error during creation of Maven custom settings.xml");
            handler.emitError(message.toString(), e.getMessage());
            InstallLog.getInstance().error(message.append('\n').append(e.getMessage()).toString());
        } catch (SAXException e) {
            StringBuilder message = new StringBuilder(
                    "XML parsing error during creation of Maven custom settings.xml");
            handler.emitError(message.toString(), e.getMessage());
            InstallLog.getInstance().error(message.append('\n').append(e.getMessage()).toString());
        }

        Properties mvnProperties = new Properties();
        mvnProperties.setProperty("conf.directory", confDirectory);
        mvnProperties.setProperty("log.directory", logsDirectory);
        mvnProperties.setProperty("bundles.directory", bundlesDirectory);
        mavenUtils.mvnCleanPackageWithProperties(
                installPath + File.separator + artifactId, mvnProperties, customMavenProxySettings);

        if (isProxyEnabled && mavenProxyAutoconf) {
            FileSystemUtils.delete(customMavenProxySettings);
        }

        switch (selectedContainer) {
            case TOMCAT:
                Tomcat tomcat = new Tomcat(
                        tomcatSsl, tomcatHost, tomcatPort, installPath, artifactId, tomcatUser, tomcatPassword,
                        handler);
                boolean deployCoreResult = tomcat.deployCore();
                if (deployCoreResult) {
                    handler.logOutput("Core successfully deployed ", true);
                    InstallLog.getInstance().info("Core successfully deployed ");
                } else {
                    String messageError = "Core deploy failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }

                boolean deployConsoleResult = tomcat.deployConsole();
                if (deployConsoleResult) {
                    handler.logOutput("Console successfully deployed ", true);
                    InstallLog.getInstance().info("Console successfully deployed ");
                } else {
                    final String messageError = "Console deploy failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }

                boolean deployEnduserResult = tomcat.deployEnduser();
                if (deployEnduserResult) {
                    handler.logOutput("Enduser successfully deployed ", true);
                    InstallLog.getInstance().info("Enduser successfully deployed ");
                } else {
                    final String messageError = "Enduser deploy failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }
                break;

            case JBOSS:
                JBoss jBoss = new JBoss(
                        jbossSsl, jbossHost, jbossManagementPort, jbossAdminUsername,
                        jbossAdminPassword, installPath, artifactId, handler);

                boolean deployCoreJboss = jBoss.deployCore();
                if (deployCoreJboss) {
                    handler.logOutput("Core successfully deployed ", true);
                    InstallLog.getInstance().info("Core successfully deployed ");
                } else {
                    String messageError = "Core deploy failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }

                boolean deployConsoleJBoss = jBoss.deployConsole();
                if (deployConsoleJBoss) {
                    handler.logOutput("Console successfully deployed ", true);
                    InstallLog.getInstance().info("Console successfully deployed ");
                } else {
                    final String messageError = "Console deploy failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }

                boolean deployEnduserJBoss = jBoss.deployEnduser();
                if (deployEnduserJBoss) {
                    handler.logOutput("Enduser successfully deployed ", true);
                    InstallLog.getInstance().info("Enduser successfully deployed ");
                } else {
                    final String messageError = "Enduser deploy failed";
                    handler.emitError(messageError, messageError);
                    InstallLog.getInstance().error(messageError);
                }
                break;

            case GLASSFISH:
                String createJavaOptCommand = "sh " + glassfishDir + Glassfish.CREATE_JAVA_OPT_COMMAND;
                fileSystemUtils.exec(createJavaOptCommand, null);

                Glassfish glassfish = new Glassfish(installPath, artifactId);

                fileSystemUtils.exec("sh " + glassfishDir
                        + Glassfish.DEPLOY_COMMAND + glassfish.deployCore(), null);
                fileSystemUtils.exec("sh " + glassfishDir
                        + Glassfish.DEPLOY_COMMAND + glassfish.deployConsole(), null);
                fileSystemUtils.exec("sh " + glassfishDir
                        + Glassfish.DEPLOY_COMMAND + glassfish.deployEnduser(), null);
                break;

            default:
        }
    }

    private void persistenceContextEMFactory(
            final FileSystemUtils fileSystemUtils, final AbstractUIProcessHandler handler) {

        fileSystemUtils.copyFileFromResources("/MasterDomain.xml",
                syncopeInstallDir + PROPERTIES.getProperty("masterDomainFile"), handler);
        File masterDomainFile = new File(
                syncopeInstallDir + PROPERTIES.getProperty("masterDomainFile"));
        String contentPersistenceContextEMFactory = fileSystemUtils.readFile(masterDomainFile);
        fileSystemUtils.writeToFile(masterDomainFile,
                contentPersistenceContextEMFactory.replace(MasterDomainXml.PLACEHOLDER, MasterDomainXml.JBOSS));
    }

}
