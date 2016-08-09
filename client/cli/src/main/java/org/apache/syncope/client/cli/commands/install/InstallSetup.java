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
package org.apache.syncope.client.cli.commands.install;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import javax.ws.rs.ProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.util.FileSystemUtils;
import org.apache.syncope.client.cli.util.JasyptUtils;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallSetup {

    private static final Logger LOG = LoggerFactory.getLogger(InstallSetup.class);

    private final InstallResultManager installResultManager = new InstallResultManager();

    private String syncopeAdminUser;

    private String syncopeAdminPassword;

    private String syncopeServerSchema;

    private String syncopeServerHostname = "localhost";

    private String syncopeServerPort = "8080";

    private String syncopeServerRestContext = "/syncope/rest/";

    public void setup() throws FileNotFoundException, IllegalAccessException {
        installResultManager.printWelcome();

        System.out.println("Path to config files of Syncope CLI client will be: "
                + InstallConfigFileTemplate.dirPath());

        if (!FileSystemUtils.exists(InstallConfigFileTemplate.dirPath())) {
            throw new FileNotFoundException("Directory: " + InstallConfigFileTemplate.dirPath() + " does not exists!");
        }

        if (!FileSystemUtils.canWrite(InstallConfigFileTemplate.dirPath())) {
            throw new IllegalAccessException("Permission denied on " + InstallConfigFileTemplate.dirPath());
        }
        System.out.println("- File system permission checked");
        System.out.println("");

        try (Scanner scanIn = new Scanner(System.in)) {
            System.out.print("Syncope server schema [http/https]: ");
            String syncopeServerSchemaFromSystemIn = scanIn.nextLine();
            boolean schemaFound = false;
            while (!schemaFound) {
                if (("http".equalsIgnoreCase(syncopeServerSchemaFromSystemIn))
                        || ("https".equalsIgnoreCase(syncopeServerSchemaFromSystemIn))) {
                    syncopeServerSchema = syncopeServerSchemaFromSystemIn;
                    schemaFound = true;
                } else {
                    System.out.println("Please use one of below values: ");
                    System.out.println("   - http");
                    System.out.println("   - https");
                    syncopeServerSchemaFromSystemIn = scanIn.nextLine();
                }
            }

            System.out.print("Syncope server hostname [e.g. " + syncopeServerHostname + "]: ");
            String syncopeServerHostnameFromSystemIn = scanIn.nextLine();
            boolean syncopeServerHostnameFound = false;
            while (!syncopeServerHostnameFound) {
                if (StringUtils.isNotBlank(syncopeServerHostnameFromSystemIn)) {
                    syncopeServerHostname = syncopeServerHostnameFromSystemIn;
                    syncopeServerHostnameFound = true;
                } else {
                    System.out.print("Syncope server hostname [e.g. " + syncopeServerHostname + "]: ");
                    syncopeServerHostnameFromSystemIn = scanIn.nextLine();
                }
            }

            System.out.print("Syncope server port [e.g. " + syncopeServerPort + "]: ");
            String syncopeServerPortFromSystemIn = scanIn.nextLine();
            boolean syncopeServerPortFound = false;
            while (!syncopeServerPortFound) {
                if (StringUtils.isNotBlank(syncopeServerPortFromSystemIn)) {
                    syncopeServerPort = syncopeServerPortFromSystemIn;
                    syncopeServerPortFound = true;
                } else if (!StringUtils.isNumeric(syncopeServerPortFromSystemIn)) {
                    System.err.println(syncopeServerPortFromSystemIn + " is not a numeric string, try again");
                    syncopeServerPortFromSystemIn = scanIn.nextLine();
                } else {
                    System.out.print("Syncope server port [e.g. " + syncopeServerPort + "]: ");
                    syncopeServerPortFromSystemIn = scanIn.nextLine();
                }
            }

            System.out.print("Syncope server rest context [e.g. " + syncopeServerRestContext + "]: ");
            String syncopeServerRestContextFromSystemIn = scanIn.nextLine();
            boolean syncopeServerRestContextFound = false;
            while (!syncopeServerRestContextFound) {
                if (StringUtils.isNotBlank(syncopeServerRestContextFromSystemIn)) {
                    syncopeServerRestContext = syncopeServerRestContextFromSystemIn;
                    syncopeServerRestContextFound = true;
                } else {
                    System.out.print("Syncope server port [e.g. " + syncopeServerRestContext + "]: ");
                    syncopeServerRestContextFromSystemIn = scanIn.nextLine();
                }
            }

            System.out.print("Syncope admin user: ");
            String syncopeAdminUserFromSystemIn = scanIn.nextLine();
            boolean syncopeAdminUserFound = false;
            while (!syncopeAdminUserFound) {
                if (StringUtils.isNotBlank(syncopeAdminUserFromSystemIn)) {
                    syncopeAdminUser = syncopeAdminUserFromSystemIn;
                    syncopeAdminUserFound = true;
                } else {
                    System.out.print("Syncope admin user: ");
                    syncopeAdminUserFromSystemIn = scanIn.nextLine();
                }
            }

            char[] syncopeAdminPasswordFromSystemConsole = System.console().readPassword("Syncope admin password: ");
            boolean syncopeAdminPasswordFound = false;
            while (!syncopeAdminPasswordFound) {
                if (syncopeAdminPasswordFromSystemConsole != null && syncopeAdminPasswordFromSystemConsole.length > 0) {
                    syncopeAdminPassword = new String(syncopeAdminPasswordFromSystemConsole);
                    syncopeAdminPasswordFound = true;
                } else {
                    syncopeAdminPasswordFromSystemConsole = System.console().readPassword("Syncope admin password: ");
                }
            }
        }

        final JasyptUtils jasyptUtils = JasyptUtils.get();
        try {

            final String contentCliPropertiesFile = InstallConfigFileTemplate.cliPropertiesFile(
                    syncopeServerSchema,
                    syncopeServerHostname,
                    syncopeServerPort,
                    syncopeServerRestContext,
                    syncopeAdminUser,
                    jasyptUtils.encrypt(syncopeAdminPassword));
            FileSystemUtils.createFileWith(InstallConfigFileTemplate.configurationFilePath(), contentCliPropertiesFile);
        } catch (final IOException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            final SyncopeService syncopeService = SyncopeServices.get(SyncopeService.class);
            final String syncopeVersion = syncopeService.platform().getVersion();
            installResultManager.installationSuccessful(syncopeVersion);
        } catch (final ProcessingException ex) {
            LOG.error("Error installing CLI", ex);
            installResultManager.manageProcessingException(ex);
        } catch (final Exception e) {
            LOG.error("Error installing CLI", e);
            installResultManager.manageException(e);
        }
    }
}
