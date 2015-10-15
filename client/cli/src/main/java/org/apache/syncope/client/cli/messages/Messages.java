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
package org.apache.syncope.client.cli.messages;

public final class Messages {

    private static final String OPTION_COMMAND_MESSAGE_TEMPLATE = "\n - Usage: %s\n";

    private static final String DELETED_MESSAGE_TEMPLATE = "%s %s successfully deleted";

    private static final String DOESNT_EXIST_MESSAGE_TEMPLATE = "%s %s doesn't exist";

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

    public static void printDeletedMessage(final String what, final String key) {
        printMessage(String.format(DELETED_MESSAGE_TEMPLATE, what, key));
    }

    private Messages() {

    }
}
