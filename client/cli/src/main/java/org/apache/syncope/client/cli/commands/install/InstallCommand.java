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
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.ws.rs.ProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.apache.syncope.client.cli.commands.LoggerCommand;
import org.apache.syncope.client.cli.util.FileSystemUtils;
import org.apache.syncope.client.cli.util.JasyptUtils;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "install")
public class InstallCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCommand.class);

    private static final String HELP_MESSAGE = "Usage: install [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --setup";

    private String syncopeAdminUser;

    private String syncopeAdminPassword;

    private String syncopeServerSchema;

    private String syncopeServerHostname = "localhost";

    private String syncopeServerPort = "8080";

    private String syncopeServerRestContext = "/syncope/rest/";

    @Override
    public void execute(final Input input) {
        LOG.debug("Option: {}", input.getOption());
        LOG.debug("Parameters:");
        for (final String parameter : input.getParameters()) {
            LOG.debug("   > " + parameter);
        }

        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        switch (Options.fromName(input.getOption())) {
            case INSTALL:
                final Scanner scanIn = new Scanner(System.in);

                System.out.println("");
                System.out.println("###############################################");
                System.out.println("#                                             #");
                System.out.println("# Welcome to Syncope CLI installation process #");
                System.out.println("#                                             #");
                System.out.println("###############################################");
                System.out.println("");

                System.out.println("Path to config files of Syncope CLI client will be: "
                        + InstallConfigFileTemplate.DIR_PATH);

                if (!FileSystemUtils.canWrite(InstallConfigFileTemplate.DIR_PATH)) {
                    System.out.println("Permission denied on " + InstallConfigFileTemplate.DIR_PATH);
                    break;
                }
                System.out.println("- File system permission checked");
                System.out.println("");

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
                    System.out.println("Provided parameters checked on Syncope core version: "
                            + syncopeService.info().getVersion());
                    System.out.println("");
                    System.out.println("###############################################");
                    System.out.println("#                                             #");
                    System.out.println("#           Installation successful           #");
                    System.out.println("#     now you can use Syncope CLI client      #");
                    System.out.println("#                                             #");
                    System.out.println("###############################################");
                    System.out.println("");
                } catch (final ProcessingException ex) {
                    if (ex.getCause() instanceof UnknownHostException) {
                        final String unknownHost = ex.getCause().getMessage().split(":")[3];
                        System.out.println("");
                        System.out.println("Provided host:" + unknownHost);
                        System.out.println("");
                        System.out.println("###############################################");
                        System.out.println("#                                             #");
                        System.out.println("#            Provided unknown host!           #");
                        System.out.println("#        START AGAIN the installation!        #");
                        System.out.println("#                                             #");
                        System.out.println("###############################################");
                        System.out.println("");
                    } else if (ex.getCause() instanceof ConnectException) {
                        System.out.println("");
                        System.out.println("Provided address :" + SyncopeServices.getAddress());
                        System.out.println("");
                        System.out.println("###############################################");
                        System.out.println("#                                             #");
                        System.out.println("#       Provided address is unreachable!      #");
                        System.out.println("#         Check it and if it is wrong         #");
                        System.out.println("#        START AGAIN the installation!        #");
                        System.out.println("#                                             #");
                        System.out.println("###############################################");
                        System.out.println("");
                    }
                } catch (final Exception e) {
                    if (e.getMessage().contains("not authenticated")) {
                        System.out.println("");
                        System.out.println("###############################################");
                        System.out.println("#                                             #");
                        System.out.println("#   Username or password provided are wrong   #");
                        System.out.println("#        START AGAIN the installation!        #");
                        System.out.println("#                                             #");
                        System.out.println("###############################################");
                        System.out.println("");
                    } else {
                        System.out.println("");
                        System.out.println("###############################################");
                        System.out.println("#                                             #");
                        System.out.println("#                Something wrong              #");
                        System.out.println("#        START AGAIN the installation!        #");
                        System.out.println("#                                             #");
                        System.out.println("###############################################");
                        System.out.println("");
                    }
                }
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                System.out.println(input.getOption() + " is not a valid option.");
                System.out.println("");
                System.out.println(HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum Options {

        HELP("--help"),
        INSTALL("--setup");

        private final String optionName;

        Options(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static Options fromName(final String name) {
            Options optionToReturn = HELP;
            for (final Options option : Options.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }
    }

}
