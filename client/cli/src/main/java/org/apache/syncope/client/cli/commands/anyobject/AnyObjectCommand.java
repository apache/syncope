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
package org.apache.syncope.client.cli.commands.anyobject;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "anyObject")
public class AnyObjectCommand extends AbstractCommand {

    private final AnyObjectResultManager anyObjectResultManager = new AnyObjectResultManager();

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(AnyObjectOptions.HELP.getOptionName());
        }
        switch (AnyObjectOptions.fromName(input.getOption())) {
            case DETAILS:
                break;
            case LIST:
                new AnyObjectList(input).list();
                break;
            case READ:
                new AnyObjectRead(input).read();
                break;
            case READ_ATTRIBUTES_BY_SCHEMA:
                new AnyObjectReadAttributeBySchemaTypeAndSchemaKey(input).read();
                break;
            case READ_ATTRIBUTES_BY_SCHEMA_TYPE:
                new AnyObjectReadAttributesBySchemaType(input).read();
                break;
            case DELETE:
                new AnyObjectDelete(input).delete();
                break;
            case HELP:
                System.out.println(getHelpMessage());
                break;
            default:
                anyObjectResultManager.defaultOptionMessage(input.getOption(), getHelpMessage());
        }
    }

    @Override
    public String getHelpMessage() {
        return anyObjectResultManager.commandHelpMessage(getClass());
    }

    private enum AnyObjectOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        READ("--read"),
        READ_ATTRIBUTES_BY_SCHEMA("--read-attr-by-schema"),
        READ_ATTRIBUTES_BY_SCHEMA_TYPE("--read-attr-by-schema-type"),
        DELETE("--delete");

        private final String optionName;

        AnyObjectOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static AnyObjectOptions fromName(final String name) {
            AnyObjectOptions optionToReturn = HELP;
            for (final AnyObjectOptions option : AnyObjectOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final AnyObjectOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
