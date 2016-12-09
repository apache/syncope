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
package org.apache.syncope.client.cli.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.CommandClassScanner;
import org.apache.syncope.client.cli.commands.AbstractCommand;

public final class CommandUtils {

    public static AbstractCommand fromArgs(final String arg)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException {

        final CommandClassScanner ccs = new CommandClassScanner();
        final List<Class<? extends AbstractCommand>> commands = ccs.getComponentClasses();

        Class<? extends AbstractCommand> commandClass = null;
        for (final Class<? extends AbstractCommand> cmd : commands) {
            if (arg.equals(cmd.getAnnotation(Command.class).name())) {
                commandClass = cmd;
            }
        }

        if (commandClass == null) {
            throw new IllegalArgumentException(arg + " is not a valid command");
        }

        return commandClass.newInstance();
    }

    public static List<AbstractCommand> commands()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException {

        final List<AbstractCommand> listCommands = new ArrayList<>();

        final CommandClassScanner ccs = new CommandClassScanner();
        final List<Class<? extends AbstractCommand>> commands = ccs.getComponentClasses();

        for (final Class<? extends AbstractCommand> cmd : commands) {
            if (cmd == null) {
                throw new IllegalArgumentException();
            }
            listCommands.add(cmd.newInstance());
        }

        return listCommands;
    }

    public static String[] fromEnumToArray(final Class<? extends Enum<?>> enumClass) {
        final String[] types = new String[enumClass.getFields().length];
        for (int i = 0; i < enumClass.getFields().length; i++) {
            types[i] = enumClass.getFields()[i].getName();
        }
        return types;
    }

    public static String helpMessage(final String command, final List<String> options) {
        final StringBuilder helpMessageBuilder = new StringBuilder(String.format("%nUsage: %s [options]%n", command));
        helpMessageBuilder.append("  Options:\n");
        for (final String option : options) {
            helpMessageBuilder.append("    ").append(option).append("\n");
        }
        return helpMessageBuilder.toString();
    }

    private CommandUtils() {

    }
}
