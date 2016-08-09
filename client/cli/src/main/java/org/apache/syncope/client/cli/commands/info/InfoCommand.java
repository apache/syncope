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
package org.apache.syncope.client.cli.commands.info;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.apache.syncope.client.cli.util.CommandUtils;

@Command(name = "info")
public class InfoCommand extends AbstractCommand {

    @Override
    public void execute(final Input input) {
        final Info info = new Info();
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        switch (Options.fromName(input.getOption())) {
            case VERSION:
                info.version();
                break;
            case PWD_RESET_ALLOWED:
                info.pwdResetAllowed();
                break;
            case PWD_RESET_WITH_SECURITY_QUESTION:
                info.resetWithSecurityQuestion();
                break;
            case SELF_REG_ALLOWED:
                info.selfRegistrationAllowed();
                break;
            case PROVISIONING_MANAGER:
                info.provisioningManager();
                break;
            case WORKFLOW_ADAPTER:
                info.workflowAdapter();
                break;
            case ACCOUNT_RULES:
                info.accountRules();
                break;
            case CONNID_LOCATION:
                info.connidLocations();
                break;
            case RECONCILIATION_FILTER_BUILDERS:
                info.reconciliationFilterBuilders();
                break;
            case LOGIC_ACTIONS:
                info.logicActions();
                break;
            case MAPPING_ITEM_TRANSFORMERS:
                info.mappingItemTransformers();
                break;
            case PASSWORD_RULES:
                info.passwordRules();
                break;
            case PROPAGATION_ACTIONS:
                info.propagationActions();
                break;
            case PUSH_ACTIONS:
                info.pushActions();
                break;
            case PUSH_CORRELATION_ACTIONS:
                info.pushCorrelationActions();
                break;
            case REPORTLET_CONFS:
                info.reportletConfs();
                break;
            case SYNC_ACTIONS:
                info.pullActions();
                break;
            case SYNC_CORRELATION_RULES:
                info.pullCorrelationRules();
                break;
            case TASK_JOBS:
                info.taskJobs();
                break;
            case VALIDATORS:
                info.validators();
                break;
            case PASSWORD_GENERATOR:
                info.passwordGenerators();
                break;
            case VIR_ATTR_CACHE:
                info.virAttrCache();
                break;
            case HELP:
                System.out.println(CommandUtils.helpMessage("info", Options.toList()));
                break;
            default:
                new InfoResultManager().defaultOptionMessage(
                        input.getOption(), CommandUtils.helpMessage("info", Options.toList()));
                break;
        }
    }

    @Override
    public String getHelpMessage() {
        return CommandUtils.helpMessage("info", Options.toList());
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
        RECONCILIATION_FILTER_BUILDERS("--reconciliation-filter-builders"),
        LOGIC_ACTIONS("--logic-actions"),
        MAPPING_ITEM_TRANSFORMERS("--mapping-item-transformers"),
        PASSWORD_RULES("--password-rules"),
        PROPAGATION_ACTIONS("--propagation-actions"),
        PUSH_ACTIONS("--push-actions"),
        PUSH_CORRELATION_ACTIONS("--push-correlation-actions"),
        REPORTLET_CONFS("--reportletConfs"),
        SYNC_ACTIONS("--sync-actions"),
        SYNC_CORRELATION_RULES("--sync-correlation-rules"),
        TASK_JOBS("--task-jobs"),
        VALIDATORS("--validators"),
        PASSWORD_GENERATOR("--password-generator"),
        VIR_ATTR_CACHE("--vir-attr-cache"),
        HELP("--help");

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
