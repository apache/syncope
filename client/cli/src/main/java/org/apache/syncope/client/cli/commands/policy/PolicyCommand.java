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
package org.apache.syncope.client.cli.commands.policy;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "policy")
public class PolicyCommand extends AbstractCommand {

    private final PolicyResultManager policyResultManager = new PolicyResultManager();

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(PolicyOptions.HELP.getOptionName());
        }

        switch (PolicyOptions.fromName(input.getOption())) {
            case DETAILS:
                new PolicyDetails(input).details();
                break;
            case LIST:
                new PolicyList(input).list();
                break;
            case READ:
                new PolicyRead(input).read();
                break;
            case DELETE:
                new PolicyDelete(input).delete();
                break;
            case HELP:
                System.out.println(getHelpMessage());
                break;
            default:
                policyResultManager.defaultOptionMessage(input.getOption(), getHelpMessage());
        }
    }

    @Override
    public String getHelpMessage() {
        return policyResultManager.commandHelpMessage(getClass());
    }

    public enum PolicyOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        READ("--read"),
        DELETE("--delete");

        private final String optionName;

        PolicyOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static PolicyOptions fromName(final String name) {
            PolicyOptions optionToReturn = HELP;
            for (final PolicyOptions option : PolicyOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final PolicyOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
