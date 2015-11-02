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
package org.apache.syncope.client.cli.commands.resource;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "resource")
public class ResourceCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "\nUsage: resource [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --details \n"
            + "    --list \n"
            + "    --read \n"
            + "       Syntax: --read {CONNECTOR-ID} {CONNECTOR-ID} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {CONNECTOR-ID} {CONNECTOR-ID} [...]\n";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(ResourceOptions.HELP.getOptionName());
        }

        switch (ResourceOptions.fromName(input.getOption())) {
            case DETAILS:
                new ResourceDetails(input).details();
                break;
            case LIST:
                new ResourceList(input).list();
                break;
            case READ:
                new ResourceRead(input).read();
                break;
            case DELETE:
                new ResourceDelete(input).delete();
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new ResourceResultManager().defaultOptionMessage(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum ResourceOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        READ("--read"),
        DELETE("--delete");

        private final String optionName;

        ResourceOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static ResourceOptions fromName(final String name) {
            ResourceOptions optionToReturn = HELP;
            for (final ResourceOptions option : ResourceOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final ResourceOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
