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
package org.apache.syncope.client.cli.view;

import java.util.List;
import java.util.ResourceBundle;

public final class Messages {

    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("messages");

    private static final String OPTION_COMMAND_MESSAGE_TEMPLATE = "%n - Usage: %s%n";

    private static final String CREATED_MESSAGE_TEMPLATE = "%s %s successfully created";

    private static final String UPDATED_MESSAGE_TEMPLATE = "%s %s successfully updated";

    private static final String DELETED_MESSAGE_TEMPLATE = "%s %s successfully deleted";

    private static final String DOESNT_EXIST_MESSAGE_TEMPLATE = "%s %s doesn't exist";

    private static final String TYPE_NOT_VALID_MESSAGE_TEMPLATE = "%s isn't a valid %s type, try with: %s";

    private static final String ID_NOT_NUMBER_MESSAGE_TEMPLATE = "Error reading %s. It isn't a valid %s "
            + "id because it isn't a long value";

    private static final String NOT_BOOLEAN_MESSAGE_TEMPLATE = "Error reading %s. It isn't a valid %s "
            + "value because it isn't a boolean value";

    private static final String DEFAULT_MESSAGE_TEMPLATE = "%s is not a valid option. %n\b %s";

    public static void printCommandOptionMessage(final String message) {
        System.out.println(String.format(OPTION_COMMAND_MESSAGE_TEMPLATE, message));
    }

    public static void printMessage(final String... messages) {
        final StringBuilder messageBuilder = new StringBuilder("\n");
        for (final String message : messages) {
            messageBuilder.append(" - ").append(message).append("\n");
        }
        System.out.println(messageBuilder.toString());
    }

    public static void printNofFoundMessage(final String what, final String key) {
        printMessage(String.format(DOESNT_EXIST_MESSAGE_TEMPLATE, what, key));
    }

    public static void printCreatedMessage(final String what, final String key) {
        printMessage(String.format(CREATED_MESSAGE_TEMPLATE, what, key));
    }

    public static void printUpdatedMessage(final String what, final String key) {
        printMessage(String.format(UPDATED_MESSAGE_TEMPLATE, what, key));
    }

    public static void printDeletedMessage(final String what, final String key) {
        printMessage(String.format(DELETED_MESSAGE_TEMPLATE, what, key));
    }

    public static void printIdNotNumberDeletedMessage(final String what, final String key) {
        printMessage(String.format(ID_NOT_NUMBER_MESSAGE_TEMPLATE, key, what));
    }

    public static void printNotBooleanDeletedMessage(final String what, final String key) {
        printMessage(String.format(NOT_BOOLEAN_MESSAGE_TEMPLATE, key, what));
    }

    public static void printTypeNotValidMessage(final String what, final String key, final String[] types) {
        final StringBuilder typesBuilder = new StringBuilder();
        for (final String type : types) {
            typesBuilder.append("\n     *** ").append(type);
        }
        printMessage(String.format(TYPE_NOT_VALID_MESSAGE_TEMPLATE, key, what, typesBuilder.toString()));
    }

    public static void printDefaultMessage(final String option, final String helpMessage) {
        printMessage(String.format(DEFAULT_MESSAGE_TEMPLATE, option, helpMessage));
    }

    public static void printUnnecessaryParameters(final List<String> parameters, final String helpMessage) {
        printMessage("Unnecessary parameter: " + parameters, "Usage: " + helpMessage);
    }

    public static String commandHelpMessage(final String name) {
        return MESSAGES.getString(name + ".help.message");
    }

    private Messages() {

    }
}
