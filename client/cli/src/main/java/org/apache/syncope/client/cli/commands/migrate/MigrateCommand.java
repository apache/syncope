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
package org.apache.syncope.client.cli.commands.migrate;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "migrate")
public class MigrateCommand extends AbstractCommand {

    private final MigrateResultManager migrateResultManager = new MigrateResultManager();

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        switch (Options.fromName(input.getOption())) {
            case CONF:
                new MigrateConf(input).migrate();
                break;

            case HELP:
                System.out.println(getHelpMessage());
                break;

            default:
                migrateResultManager.defaultOptionMessage(input.getOption(), getHelpMessage());
        }
    }

    @Override
    public String getHelpMessage() {
        return migrateResultManager.commandHelpMessage(getClass());
    }

    private enum Options {

        HELP("--help"),
        CONF("--conf");

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
