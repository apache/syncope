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

import org.apache.syncope.client.cli.commands.logger.LoggerCommand;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "domain")
public class DomainCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCommand.class);

    private static final String HELP_MESSAGE = "Usage: domain [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list \n"
            + "    --read \n"
            + "       Syntax: --read {DOMAIN-KEY} {DOMAIN-KEY} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {DOMAIN-KEY} {DOMAIN-KEY} [...]\n";

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

        final DomainService domainService = SyncopeServices.get(DomainService.class);
        switch (Options.fromName(input.getOption())) {
            case LIST:
                try {
                    for (final DomainTO domainTO : domainService.list()) {
                        Messages.printMessage("Domain key: " + domainTO.getKey());
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage("Error: " + ex.getMessage());
                }
                break;
            case READ:
                final String readErrorMessage = "domain --read {DOMAIN-KEY} {DOMAIN-KEY} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            final DomainTO domainTO = domainService.read(parameter);
                            Messages.printMessage("Domain key: " + domainTO.getKey());
                        } catch (final SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Domain", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(readErrorMessage);
                }
                break;
            case DELETE:
                final String deleteErrorMessage = "domain --delete {DOMAIN-KEY} {DOMAIN-KEY} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            domainService.delete(parameter);
                            Messages.printDeletedMessage("Domain", parameter);
                        } catch (final SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Domain", parameter);
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
