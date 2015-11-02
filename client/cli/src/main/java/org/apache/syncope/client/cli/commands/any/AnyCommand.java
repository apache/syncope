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
package org.apache.syncope.client.cli.commands.any;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "any")
public class AnyCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "\nUsage: any [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --details \n"
            + "    --list \n"
            + "    --read \n"
            + "       Syntax: --read {ANY-ID} {ANY-ID} [...]\n"
            + "    --read-attr-by-schema-type {ANY-ID} {SCHEMA-TYPE}\n"
            + "       Schema type: PLAIN / DERIVED / VIRTUAL\n"
            + "    --read-attr-by-schema {ANY-ID} {SCHEMA-TYPE} {SCHEMA-NAME}\n"
            + "       Schema type: PLAIN / DERIVED / VIRTUAL\n"
            + "    --delete \n"
            + "       Syntax: --delete {ANY-ID} {ANY-ID} [...]\n";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(AnyOptions.HELP.getOptionName());
        }
        switch (AnyOptions.fromName(input.getOption())) {
            case DETAILS:
                break;
            case LIST:
                new AnyList(input).list();
                break;
            case READ:
                new AnyRead(input).read();
                break;
            case READ_ATTRIBUTES_BY_SCHEMA:
                new AnyReadAttributeBySchemaTypeAndSchemaName(input).read();
                break;
            case READ_ATTRIBUTES_BY_SCHEMA_TYPE:
                new AnyReadAttributesBySchemaType(input).read();
                break;
            case DELETE:
                new AnyDelete(input).delete();
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new AnyResultManager().defaultOptionMessage(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum AnyOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        READ("--read"),
        READ_ATTRIBUTES_BY_SCHEMA("--read-attr-by-schema"),
        READ_ATTRIBUTES_BY_SCHEMA_TYPE("--read-attr-by-schema-type"),
        DELETE("--delete");

        private final String optionName;

        AnyOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static AnyOptions fromName(final String name) {
            AnyOptions optionToReturn = HELP;
            for (final AnyOptions option : AnyOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final AnyOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
