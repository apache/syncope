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
package org.apache.syncope.client.cli.commands.entitlement;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "entitlement")
public class EntitlementCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "Usage: entitlement [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list\n"
            + "    --list-role\n"
            + "       Syntax: --list-role {ENTITLEMENT-NAME}\n"
            + "    --read-by-username\n"
            + "       Syntax: --read-by-username {USERNAME}\n"
            + "    --read-by-userid\n"
            + "       Syntax: --read-by-userid {USER-ID}\n"
            + "    --search-by-role\n"
            + "       Syntax: --search-by-role {ROLE-ID}";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(EntitlementOptions.HELP.getOptionName());
        }

        switch (EntitlementOptions.fromName(input.getOption())) {
            case LIST:
                new EntitlementList(input).list();
                break;
            case READ_BY_USERNAME:
                new EntitlementReadByUsername(input).read();
                break;
            case READ_BY_USERID:
                new EntitlementReadByUserId(input).read();
                break;
            case SEARCH_BY_ROLE:
                new EntitlementSearchByRole(input).search();
                break;
            case LIST_ROLE:
                new EntitlementListRole(input).list();
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new EntitlementResultManager().defaultError(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum EntitlementOptions {

        HELP("--help"),
        LIST("--list"),
        READ_BY_USERNAME("--read-by-username"),
        READ_BY_USERID("--read-by-userid"),
        SEARCH_BY_ROLE("--search-by-role"),
        LIST_ROLE("--list-role");

        private final String optionName;

        EntitlementOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static EntitlementOptions fromName(final String name) {
            EntitlementOptions optionToReturn = HELP;
            for (final EntitlementOptions option : EntitlementOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final EntitlementOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
