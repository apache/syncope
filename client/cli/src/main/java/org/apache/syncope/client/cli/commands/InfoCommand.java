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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.common.lib.to.SyncopeTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "info")
public class InfoCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(InfoCommand.class);

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

        final SyncopeService syncopeService = SyncopeServices.get(SyncopeService.class);
        final SyncopeTO syncopeTO = syncopeService.info();
        switch (Options.fromName(input.getOption())) {
            case VERSION:
                try {
                    Messages.printMessage("Syncope version: " + syncopeTO.getVersion());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PWD_RESET_ALLOWED:
                try {
                    Messages.printMessage("Password reset allowed: " + syncopeTO.isPwdResetAllowed());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PWD_RESET_WITH_SECURITY_QUESTION:
                try {
                    Messages.printMessage("Password reset requiring security question: "
                            + syncopeTO.isPwdResetRequiringSecurityQuestions());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case SELF_REG_ALLOWED:
                try {
                    Messages.printMessage("Self registration allowed: " + syncopeTO.isSelfRegAllowed());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PROVISIONING_MANAGER:
                try {
                    Messages.printMessage(
                            "Any object provisioning manager class: " + syncopeTO.getAnyObjectProvisioningManager(),
                            "User       provisioning manager class: " + syncopeTO.getUserProvisioningManager(),
                            "Group      provisioning manager class: " + syncopeTO.getGroupProvisioningManager());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case WORKFLOW_ADAPTER:
                try {
                    Messages.printMessage(
                            "Any object workflow adapter class: " + syncopeTO.getAnyObjectWorkflowAdapter(),
                            "User       workflow adapter class: " + syncopeTO.getUserWorkflowAdapter(),
                            "Group      workflow adapter class: " + syncopeTO.getGroupWorkflowAdapter());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case ACCOUNT_RULES:
                try {
                    for (final String accountRule : syncopeTO.getAccountRules()) {
                        Messages.printMessage("Account rule: " + accountRule);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case CONNID_LOCATION:
                try {
                    for (final String location : syncopeTO.getConnIdLocations()) {
                        Messages.printMessage("ConnId location: " + location);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case LOGIC_ACTIONS:
                try {
                    for (final String logic : syncopeTO.getLogicActions()) {
                        Messages.printMessage("Logic action: " + logic);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case MAIL_TEMPLATES:
                try {
                    for (final String template : syncopeTO.getMailTemplates()) {
                        Messages.printMessage("Mail template: " + template);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case MAPPING_ITEM_TRANSFORMERS:
                try {
                    for (final String tranformer : syncopeTO.getMappingItemTransformers()) {
                        Messages.printMessage("Mapping item tranformer: " + tranformer);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PASSWORD_RULES:
                try {
                    for (final String rules : syncopeTO.getPasswordRules()) {
                        Messages.printMessage("Password rule: " + rules);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PROPAGATION_ACTIONS:
                try {
                    for (final String action : syncopeTO.getPropagationActions()) {
                        Messages.printMessage("Propagation action: " + action);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PUSH_ACTIONS:
                try {
                    for (final String action : syncopeTO.getPushActions()) {
                        Messages.printMessage("Push action: " + action);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PUSH_CORRELATION_ACTIONS:
                try {
                    for (final String rule : syncopeTO.getPushCorrelationRules()) {
                        Messages.printMessage("Push correlation rule: " + rule);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case REPORTLETS:
                try {
                    for (final String reportlet : syncopeTO.getReportlets()) {
                        Messages.printMessage("Reportlet: " + reportlet);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case SYNC_ACTIONS:
                try {
                    for (final String action : syncopeTO.getSyncActions()) {
                        Messages.printMessage("Sync action: " + action);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case SYNC_CORRELATION_RULES:
                try {
                    for (final String rule : syncopeTO.getSyncCorrelationRules()) {
                        Messages.printMessage("Sync correlation rule: " + rule);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case TASK_JOBS:
                try {
                    for (final String job : syncopeTO.getTaskJobs()) {
                        Messages.printMessage("Task job: " + job);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case VALIDATORS:
                try {
                    for (final String validator : syncopeTO.getValidators()) {
                        Messages.printMessage("Validator: " + validator);
                    }
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case PASSWORD_GENERATOR:
                try {
                    Messages.printMessage(
                            "Password generator class: " + syncopeTO.getPasswordGenerator());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case VIR_ATTR_CACHE:
                try {
                    Messages.printMessage(
                            "Virtual attribute cache class: " + syncopeTO.getVirAttrCache());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case HELP:
                System.out.println(helpMessage("info", Options.toList()));
                break;
            default:
                System.out.println(input.getOption() + " is not a valid option.");
                System.out.println("");
                System.out.println(helpMessage("info", Options.toList()));
                break;
        }
    }

    @Override
    public String getHelpMessage() {
        return helpMessage("info", Options.toList());
    }

    private enum Options {

        VERSION("--version"),
        PWD_RESET_ALLOWED("--pwd-reset-allowed"),
        PWD_RESET_WITH_SECURITY_QUESTION("--pwd-reset-with-question"),
        SELF_REG_ALLOWED("--self-reg-allowed"),
        PROVISIONING_MANAGER("--provisioning-manager-classes"),
        WORKFLOW_ADAPTER("--workflow-adapter-classes"),
        ACCOUNT_RULES("--account-rules-classes"),
        CONNID_LOCATION("--connid-locations"),
        LOGIC_ACTIONS("--logic-actions"),
        MAIL_TEMPLATES("--mail-templates"),
        MAPPING_ITEM_TRANSFORMERS("--mapping-item-transformers"),
        PASSWORD_RULES("--password-rules"),
        PROPAGATION_ACTIONS("--propagation-actions"),
        PUSH_ACTIONS("--push-actions"),
        PUSH_CORRELATION_ACTIONS("--push-correlation-actions"),
        REPORTLETS("--reportlets"),
        SYNC_ACTIONS("--sync-actions"),
        SYNC_CORRELATION_RULES("--sync-correlation-rules"),
        TASK_JOBS("--task-jobs"),
        VALIDATORS("--validators"),
        PASSWORD_GENERATOR("--password-generator"),
        VIR_ATTR_CACHE("--vir-attr-cache"),
        HELP("--help");

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
