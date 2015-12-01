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
package org.apache.syncope.client.cli.commands.connector;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "connector")
public class ConnectorCommand extends AbstractCommand {

    private final ConnectorResultManager connectorResultManager = new ConnectorResultManager();

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(ConnectorOptions.HELP.getOptionName());
        }

        switch (ConnectorOptions.fromName(input.getOption())) {
            case DETAILS:
                new ConnectorDetails(input).details();
                break;
            case LIST:
                new ConnectorList(input).list();
                break;
            case LIST_BUNDLES:
                new ConnectorListBundles(input).list();
                break;
            case LIST_CONFIGURATION:
                new ConnectorListConfigurationProperties(input).list();
                break;
            case READ:
                new ConnectorRead(input).read();
                break;
            case READ_BY_RESOURCE:
                new ConnectorReadByResource(input).read();
                break;
            case DELETE:
                new ConnectorDelete(input).delete();
                break;
            case HELP:
                System.out.println(getHelpMessage());
                break;
            default:
                connectorResultManager.defaultOptionMessage(input.getOption(), getHelpMessage());
        }
    }

    @Override
    public String getHelpMessage() {
        return connectorResultManager.commandHelpMessage(getClass());
    }

    public enum ConnectorOptions {

        HELP("--help"),
        LIST("--list"),
        DETAILS("--details"),
        LIST_BUNDLES("--list-bundles"),
        LIST_CONFIGURATION("--list-configuration-properties"),
        READ("--read"),
        READ_BY_RESOURCE("--read-by-resource"),
        DELETE("--delete");

        private final String optionName;

        ConnectorOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static ConnectorOptions fromName(final String name) {
            ConnectorOptions optionToReturn = HELP;
            for (final ConnectorOptions option : ConnectorOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final ConnectorOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
