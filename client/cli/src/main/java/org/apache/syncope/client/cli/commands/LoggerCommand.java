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
import org.apache.syncope.common.lib.to.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "logger")
public class LoggerCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCommand.class);

    private static final String HELP_MESSAGE = "Usage: logger [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list \n"
            + "    --read \n"
            + "       Syntax: --read {LOG-NAME} {LOG-NAME} [...]\n"
            + "    --update \n"
            + "       Syntax: --update {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]\n"
            + "    --update-all \n"
            + "       Syntax: --update-all {LOG-LEVEL} \n"
            + "    --create \n"
            + "       Syntax: --create {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {LOG-NAME} {LOG-NAME} [...]";

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

        final LoggerService loggerService = SyncopeServices.get(LoggerService.class);
        switch (Options.fromName(input.getOption())) {
            case LIST:
                try {
                    for (final LoggerTO loggerTO : loggerService.list(LoggerType.LOG)) {
                        System.out.println(" - " + loggerTO.getKey() + " -> " + loggerTO.getLevel());
                        System.out.println("");
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage("Error: " + ex.getMessage());
                }
                break;
            case READ:
                final String readErrorMessage = "logger --read {LOG-NAME} {LOG-NAME} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            final LoggerTO loggerTO = loggerService.read(LoggerType.LOG, parameter);
                            System.out.println("\n - Logger");
                            System.out.println("   - key: " + loggerTO.getKey());
                            System.out.println("   - level: " + loggerTO.getLevel());
                            System.out.println("");
                        } catch (final SyncopeClientException | WebServiceException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Logger", parameter);
                            } else {
                                Messages.printMessage("Error: " + ex.getMessage());
                            }
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(readErrorMessage);
                }
                break;
            case UPDATE:
                final String updateErrorMessage = "logger --update {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]";

                if (parameters.length >= 1) {
                    Input.PairParameter pairParameter;
                    for (final String parameter : parameters) {
                        try {
                            pairParameter = input.toPairParameter(parameter);
                            final LoggerTO loggerTO = loggerService.read(LoggerType.LOG, pairParameter.getKey());
                            loggerTO.setLevel(LoggerLevel.valueOf(pairParameter.getValue()));
                            loggerService.update(LoggerType.LOG, loggerTO);
                            System.out.
                                    println("\n - Logger " + loggerTO.getKey() + " updated");
                            System.out.println("   - new level: " + loggerTO.getLevel());
                            System.out.println("");
                        } catch (final WebServiceException | SyncopeClientException | IllegalArgumentException ex) {
                            if (ex.getMessage().startsWith("No enum constant org.apache.syncope.common.lib.types.")) {
                                Messages.printTypeNotValidMessage(
                                        "logger level", input.firstParameter(), fromEnumToArray(LoggerLevel.class));
                            } else if ("Parameter syntax error!".equalsIgnoreCase(ex.getMessage())) {
                                Messages.printMessage(ex.getMessage(), updateErrorMessage);
                            } else if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Logger", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage(), updateErrorMessage);
                            }
                            break;
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(updateErrorMessage);
                }
                break;
            case UPDATE_ALL:
                final String updateAllErrorMessage = "logger --update-all {LOG-LEVEL}";

                if (parameters.length == 1) {
                    for (final LoggerTO loggerTO : loggerService.list(LoggerType.LOG)) {
                        try {
                            loggerTO.setLevel(LoggerLevel.valueOf(parameters[0]));
                            loggerService.update(LoggerType.LOG, loggerTO);
                            System.out.
                                    println("\n - Logger " + loggerTO.getKey() + " updated");
                            System.out.println("   - new level: " + loggerTO.getLevel());
                            System.out.println("");
                        } catch (final WebServiceException | SyncopeClientException | IllegalArgumentException ex) {
                            if (ex.getMessage().startsWith("No enum constant org.apache.syncope.common.lib.types.")) {
                                Messages.printTypeNotValidMessage(
                                        "logger level", input.firstParameter(), fromEnumToArray(LoggerLevel.class));
                            } else {
                                Messages.printMessage(ex.getMessage(), updateAllErrorMessage);
                            }
                            break;
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(updateAllErrorMessage);
                }
                break;
            case CREATE:
                final String createErrorMessage = "logger --create {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]";

                if (parameters.length >= 1) {
                    Input.PairParameter pairParameter;
                    LoggerTO loggerTO;
                    for (final String parameter : parameters) {
                        loggerTO = new LoggerTO();
                        try {
                            pairParameter = input.toPairParameter(parameter);
                            loggerTO.setKey(pairParameter.getKey());
                            loggerTO.setLevel(LoggerLevel.valueOf(pairParameter.getValue()));
                            loggerService.update(LoggerType.LOG, loggerTO);
                            System.out.
                                    println("\n - Logger " + loggerTO.getKey() + " updated");
                            System.out.println("   - level: " + loggerTO.getLevel());
                            System.out.println("");
                        } catch (final WebServiceException | SyncopeClientException | IllegalArgumentException ex) {
                            Messages.printTypeNotValidMessage(
                                    "logger level", input.firstParameter(), fromEnumToArray(LoggerLevel.class));
                            break;
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(createErrorMessage);
                }
                break;
            case DELETE:
                final String deleteErrorMessage = "logger --delete {LOG-NAME} {LOG-NAME} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            loggerService.delete(LoggerType.LOG, parameter);
                            Messages.printDeletedMessage("Logger", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Logger", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(deleteErrorMessage);
                }
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                Messages.printDefaultMessage(input.getOption(), HELP_MESSAGE);
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
        UPDATE("--update"),
        UPDATE_ALL("--update-all"),
        CREATE("--create"),
        DELETE("--delete");

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

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final Options value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }

}
