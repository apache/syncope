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
package org.apache.syncope.client.cli.commands.user;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "user")
public class UserCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "Usage: user [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list \n"
            + "    --details \n"
            + "    --get-user-key\n"
            + "       Syntax: --get-user-key {USER-ID} {USER-ID} [...]\n"
            + "    --get-username\n"
            + "       Syntax: --get-username {USERNAME} {USERNAME} [...]\n"
            + "    --read \n"
            + "       Syntax: --read {USER-ID} {USER-ID} [...]\n"
            + "    --search-by-attribute \n"
            + "       Syntax: --search-by-attribute {REALM} {ATTR-NAME}={ATTR-VALUE}\n"
            + "    --search-by-role \n"
            + "       Syntax: --search-by-role {REALM} {ROLE-ID}\n"
            + "    --search-by-resource \n"
            + "       Syntax: --search-by-resource {REALM} {RESOURCE-NAME}\n"
            + "    --delete \n"
            + "       Syntax: --delete {USER-ID} {USER-ID} [...]";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(UserOptions.HELP.getOptionName());
        }

        switch (UserOptions.fromName(input.getOption())) {
            case DETAILS:
                new UserDetails(input).details();
                break;
            case LIST:
                new UserList(input).list();
                break;
            case GET_BY_KEY:
                new UserGetKey(input).get();
                break;
            case GET_BY_USERNAME:
                new UserGetUsername(input).get();
                break;
            case READ:
                new UserRead(input).read();
                break;
            case SEARCH_BY_ATTRIBUTE:
                new UserSearchByAttribute(input).search();
                break;
            case SEARCH_BY_ROLE:
                new UserSearchByRole(input).search();
                break;
            case SEARCH_BY_RESOURCE:
                new UserSearchByResource(input).search();
                break;
            case DELETE:
                new UserDelete(input).delete();
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new UserResultManager().defaultError(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum UserOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        GET_BY_KEY("--get-user-key"),
        GET_BY_USERNAME("--get-username"),
        READ("--read"),
        SEARCH_BY_ATTRIBUTE("--search-by-attribute"),
        SEARCH_BY_ROLE("--search-by-role"),
        SEARCH_BY_RESOURCE("--search-by-resource"),
        DELETE("--delete");

        private final String optionName;

        UserOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static UserOptions fromName(final String name) {
            UserOptions optionToReturn = HELP;
            for (final UserOptions option : UserOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final UserOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
