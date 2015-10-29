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
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import javax.ws.rs.ProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.util.FileSystemUtils;
import org.apache.syncope.client.cli.util.JasyptUtils;
import org.apache.syncope.common.rest.api.service.SyncopeService;

public class InstallSetup {

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
                + InstallConfigFileTemplate.DIR_PATH);

        if (!FileSystemUtils.exists(InstallConfigFileTemplate.DIR_PATH)) {
            throw new FileNotFoundException("Directory: " + InstallConfigFileTemplate.DIR_PATH + " does not exists!");
        }

        if (!FileSystemUtils.canWrite(InstallConfigFileTemplate.DIR_PATH)) {
            throw new IllegalAccessException("Permission denied on " + InstallConfigFileTemplate.DIR_PATH);
        }
        System.out.println("- File system permission checked");
        System.out.println("");

        final Scanner scanIn = new Scanner(System.in);
        System.out.println("Syncope server schema [http/https]:");
        String syncopeServerSchemaFromSystemIn = scanIn.nextLine();
        boolean schemaFounded = false;
        while (!schemaFounded) {
            if (("http".equalsIgnoreCase(syncopeServerSchemaFromSystemIn))
                    || ("https".equalsIgnoreCase(syncopeServerSchemaFromSystemIn))) {
                syncopeServerSchema = syncopeServerSchemaFromSystemIn;
                schemaFounded = true;
            } else {
                System.out.println("Please use one of below values:");
                System.out.println("   - http");
                System.out.println("   - https");
                syncopeServerSchemaFromSystemIn = scanIn.nextLine();
            }
        }

        System.out.println("Syncope server hostname [e.g. " + syncopeServerHostname + "]:");
        String syncopeServerHostnameFromSystemIn = scanIn.nextLine();
        boolean syncopeServerHostnameFounded = false;
        while (!syncopeServerHostnameFounded) {
            if (StringUtils.isNotBlank(syncopeServerHostnameFromSystemIn)) {
                syncopeServerHostname = syncopeServerHostnameFromSystemIn;
                syncopeServerHostnameFounded = true;
            } else {
                System.out.println("Syncope server hostname [e.g. " + syncopeServerHostname + "]:");
                syncopeServerHostnameFromSystemIn = scanIn.nextLine();
            }
        }

        System.out.println("Syncope server port [e.g. " + syncopeServerPort + "]:");
        String syncopeServerPortFromSystemIn = scanIn.nextLine();
        boolean syncopeServerPortFounded = false;
        while (!syncopeServerPortFounded) {
            if (StringUtils.isNotBlank(syncopeServerPortFromSystemIn)) {
                syncopeServerPort = syncopeServerPortFromSystemIn;
                syncopeServerPortFounded = true;
            } else if (!StringUtils.isNumeric(syncopeServerPortFromSystemIn)) {
                System.out.println(syncopeServerPortFromSystemIn + " is not a numeric string, try again");
                syncopeServerPortFromSystemIn = scanIn.nextLine();
            } else {
                System.out.println("Syncope server port [e.g. " + syncopeServerPort + "]:");
                syncopeServerPortFromSystemIn = scanIn.nextLine();
            }
        }

        System.out.println("Syncope server rest context [e.g. " + syncopeServerRestContext + "]:");
        String syncopeServerRestContextFromSystemIn = scanIn.nextLine();
        boolean syncopeServerRestContextFounded = false;
        while (!syncopeServerRestContextFounded) {
            if (StringUtils.isNotBlank(syncopeServerRestContextFromSystemIn)) {
                syncopeServerRestContext = syncopeServerRestContextFromSystemIn;
                syncopeServerRestContextFounded = true;
            } else {
                System.out.println("Syncope server port [e.g. " + syncopeServerRestContext + "]:");
                syncopeServerRestContextFromSystemIn = scanIn.nextLine();
            }
        }

        System.out.println("Syncope admin user:");
        String syncopeAdminUserFromSystemIn = scanIn.nextLine();
        boolean syncopeAdminUserFounded = false;
        while (!syncopeAdminUserFounded) {
            if (StringUtils.isNotBlank(syncopeAdminUserFromSystemIn)) {
                syncopeAdminUser = syncopeAdminUserFromSystemIn;
                syncopeAdminUserFounded = true;
            } else {
                System.out.println("Syncope admin user:");
                syncopeAdminUserFromSystemIn = scanIn.nextLine();
            }
        }

        System.out.println("Syncope admin password:");
        String syncopeAdminPasswordFromSystemIn = scanIn.nextLine();
        boolean syncopeAdminPasswordFounded = false;
        while (!syncopeAdminPasswordFounded) {
            if (StringUtils.isNotBlank(syncopeAdminPasswordFromSystemIn)) {
                syncopeAdminPassword = syncopeAdminPasswordFromSystemIn;
                syncopeAdminPasswordFounded = true;
            } else {
                System.out.println("Syncope admin user:");
                syncopeAdminPasswordFromSystemIn = scanIn.nextLine();
            }
        }

        scanIn.close();

        final JasyptUtils jasyptUtils = JasyptUtils.getJasyptUtils();
        try {
            FileSystemUtils.createNewDirectory(InstallConfigFileTemplate.DIR_PATH);
            final String contentCliPropertiesFile = InstallConfigFileTemplate.createFile(
                    syncopeServerSchema,
                    syncopeServerHostname,
                    syncopeServerPort,
                    syncopeServerRestContext,
                    syncopeAdminUser,
                    jasyptUtils.encrypt(syncopeAdminPassword));
            FileSystemUtils.createFileWith(InstallConfigFileTemplate.FILE_PATH, contentCliPropertiesFile);

        } catch (final FileNotFoundException | UnsupportedEncodingException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            final SyncopeService syncopeService = SyncopeServices.get(SyncopeService.class);
            installResultManager.installationSuccessful(syncopeService.info().getVersion());
        } catch (final ProcessingException ex) {
            installResultManager.manageProcessingException(ex);
        } catch (final Exception e) {
            installResultManager.manageException(e);
        }
    }
}
