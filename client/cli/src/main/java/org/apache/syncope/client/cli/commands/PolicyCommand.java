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
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "policy")
public class PolicyCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyCommand.class);

    private static final String HELP_MESSAGE = "Usage: policy [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list-policy \n"
            + "       Syntax: --list-policy {POLICY-TYPE} \n"
            + "          Policy type: ACCOUNT / PASSWORD / SYNC / PUSH\n"
            + "    --read \n"
            + "       Syntax: --read {POLICY-ID} {POLICY-ID} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {POLICY-ID} {POLICY-ID} [...]";

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

        final PolicyService policyService = SyncopeServices.get(PolicyService.class);
        switch (Options.fromName(input.getOption())) {
            case LIST_POLICY:
                final String listPolicyErrorMessage = "policy --list-policy {POLICY-TYPE}\n"
                        + "   Policy type: ACCOUNT / PASSWORD / SYNC / PUSH";

                if (parameters.length == 1) {
                    try {
                        for (final AbstractPolicyTO policyTO : policyService.list(PolicyType.valueOf(parameters[0]))) {
                            System.out.println(policyTO);
                        }
                    } catch (final SyncopeClientException ex) {
                        Messages.printMessage(ex.getMessage());
                    } catch (final IllegalArgumentException ex) {
                        Messages.printTypeNotValidMessage(
                                "policy", input.firstParameter(), fromEnumToArray(PolicyType.class));
                    }
                } else {
                    Messages.printCommandOptionMessage(listPolicyErrorMessage);
                }
                break;
            case READ:
                final String readErrorMessage = "policy --read {POLICY-ID} {POLICY-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            System.out.println(policyService.read(Long.valueOf(parameter)));
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("policy", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Policy", parameter);
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
                final String deleteErrorMessage = "policy --delete {POLICY-ID} {POLICY-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            policyService.delete(Long.valueOf(parameter));
                            Messages.printDeletedMessage("Policy", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            System.out.println("Error:");
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Policy", parameter);
                            } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                                Messages.printMessage("You cannot delete policy " + parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("policy", parameter);
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
        LIST_POLICY("--list-policy"),
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
