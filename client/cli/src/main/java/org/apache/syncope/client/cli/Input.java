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
package org.apache.syncope.client.cli;

import java.util.List;
import org.apache.syncope.client.cli.commands.AbstractCommand;

public class Input {

    private boolean ssl = false;

    private String host = "localhost";

    private String port = "9080";

    private String username = "admin";

    private String password = "password";

    private final AbstractCommand command;

    private String option;

    private final String[] parameters;

    public Input(final String[] args)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException {

        command = fromArgs(args[0]);

        if (args.length > 1) {
            option = args[1];
        }

        if (args.length > 2) {
            parameters = new String[args.length - 2];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = args[i + 2];
            }
        } else {
            parameters = new String[0];
        }
    }

    public AbstractCommand getCommand() {
        return command;
    }

    public void setOption(final String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }

    public String[] getParameters() {
        return parameters;
    }

    public String firstParameter() {
        return parameters[0];
    }
    
    public String lastParameter() {
        return parameters[parameters.length - 1];
    }

    public PairParameter toPairParameter(final String parameter) throws IllegalArgumentException {
        if (!parameter.contains("=")) {
            throw new IllegalArgumentException("Parameter syntax error!");
        }
        final String[] pairParameterArray = parameter.split("=");
        return new PairParameter(pairParameterArray[0], pairParameterArray[1]);
    }

    private AbstractCommand fromArgs(final String arg)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException {

        final CommandClassScanner ccs = new CommandClassScanner();
        final List<Class<? extends AbstractCommand>> commands = ccs.getComponentClasses();

        Class<? extends AbstractCommand> commandClass = null;
        for (Class<? extends AbstractCommand> cmd : commands) {
            if (arg.equals(cmd.getAnnotation(Command.class).name())) {
                commandClass = cmd;
            }
        }

        if (commandClass == null) {
            throw new IllegalArgumentException();
        }

        return commandClass.newInstance();

    }

    public class PairParameter {

        private final String key;

        private final String value;

        public PairParameter(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

    }
}
