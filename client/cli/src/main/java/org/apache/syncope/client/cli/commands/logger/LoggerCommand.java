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
package org.apache.syncope.client.cli.commands.logger;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "logger")
public class LoggerCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "\nUsage: logger [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --details \n"
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
            + "       Syntax: --delete {LOG-NAME} {LOG-NAME} [...]\n";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(LoggerOptions.HELP.getOptionName());
        }

        switch (LoggerOptions.fromName(input.getOption())) {
            case DETAILS:
                new LoggerDetails(input).details();
                break;
            case LIST:
                new LoggerList(input).list();
                break;
            case READ:
                new LoggerRead(input).read();
                break;
            case UPDATE:
                new LoggerUpdate(input).update();
                break;
            case UPDATE_ALL:
                new LoggerUpdateAll(input).updateAll();
                break;
            case CREATE:
                new LoggerCreate(input).create();
                break;
            case DELETE:
                new LoggerDelete(input).delete();
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new LoggerResultManager().defaultError(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum LoggerOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        READ("--read"),
        UPDATE("--update"),
        UPDATE_ALL("--update-all"),
        CREATE("--create"),
        DELETE("--delete");

        private final String optionName;

        LoggerOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static LoggerOptions fromName(final String name) {
            LoggerOptions optionToReturn = HELP;
            for (final LoggerOptions option : LoggerOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final LoggerOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
