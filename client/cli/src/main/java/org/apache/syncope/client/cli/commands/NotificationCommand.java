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
package org.apache.syncope.client.cli.commands;

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "notification")
public class NotificationCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationCommand.class);

    private static final String HELP_MESSAGE = "Usage: notification [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list \n"
            + "    --read \n"
            + "       Syntax: --read {NOTIFICATION-ID} \n"
            + "    --delete \n"
            + "       Syntax: --delete {NOTIFICATION-ID}";

    @Override
    public void execute(final Input input) {
        LOG.debug("Option: {}", input.getOption());
        LOG.debug("Parameters:");
        for (final String parameter : input.getParameters()) {
            LOG.debug("   > " + parameter);
        }

        final String[] parameters = input.getParameters();

        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        final NotificationService notificationService = SyncopeServices.get(NotificationService.class);
        switch (Options.fromName(input.getOption())) {
            case LIST:
                try {
                    for (final NotificationTO notificationTO : notificationService.list()) {
                        System.out.println(notificationTO);
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage(ex.getMessage());
                }
                break;
            case READ:
                final String readErrorMessage = Messages.optionCommandMessage(
                        "notification --read {NOTIFICATION-ID} {NOTIFICATION-ID} [...]");
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            System.out.println(notificationService.read(Long.valueOf(parameter)));
                        } catch (final NumberFormatException ex) {
                            System.out.println("Error reading " + parameter + ". It isn't a valid notification id");
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printMessage("Notification " + parameter + " doesn't exists!");
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        }
                    }
                } else {
                    System.out.println(readErrorMessage);
                }
                break;
            case DELETE:
                final String deleteErrorMessage = Messages.optionCommandMessage(
                        "notification --delete {NOTIFICATION-ID} {NOTIFICATION-ID} [...]");

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            notificationService.delete(Long.valueOf(parameter));
                            System.out.println("\n - Notification " + parameter + " deleted!\n");
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printMessage("Notification " + parameter + " doesn't exists!");
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printMessage(
                                    "Error reading " + parameter + ". It isn't a valid notification id");
                        }
                    }
                } else {
                    System.out.println(deleteErrorMessage);
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
        LIST("--list"),
        READ("--read"),
        DELETE("--delete");

        private final String optionName;

        private Options(final String optionName) {
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
        
        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final Options value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }

}
