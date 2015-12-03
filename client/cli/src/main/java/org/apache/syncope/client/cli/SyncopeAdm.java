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

import javax.ws.rs.ProcessingException;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SyncopeAdm {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeAdm.class);

    private static final ResultManager RESULT_MANAGER = new ResultManager();

    public static void main(final String[] args) {
        LOG.debug("Starting with args \n");

        try {
            ArgsManager.validator(args);
            final Input input = new Input(args);
            final AbstractCommand command = input.getCommand();

            LOG.debug("Command: {}", command.getClass().getAnnotation(Command.class).name());
            LOG.debug("Option: {}", input.getOption());
            LOG.debug("Parameters:");
            for (final String parameter : input.getParameters()) {
                LOG.debug("   > " + parameter);
            }

            System.out.println("");
            System.out.println("You are running: " + input.printCommandFields());
            command.execute(input);
        } catch (final IllegalAccessException | InstantiationException e) {
            System.out.println(helpMessage());
        } catch (final IllegalArgumentException ex) {
            LOG.error("Error in main", ex);
            if (!ex.getMessage().startsWith("It seems you")) {
                System.out.println("");
                System.out.println(helpMessage());
            } else {
                RESULT_MANAGER.genericError(ex.getMessage());
            }
        } catch (final ProcessingException e) {
            LOG.error("Error in main", e);
            RESULT_MANAGER.genericError("Syncope server offline");
            RESULT_MANAGER.genericError(e.getCause().getMessage());
        }

    }

    private static String helpMessage() {
        final StringBuilder helpMessageBuilder = new StringBuilder("Usage: Main [options]\n");
        helpMessageBuilder.append("  Options:\n");
        try {
            for (AbstractCommand command : CommandUtils.commands()) {
                final String commandName = command.getClass().getAnnotation(Command.class).name();
                helpMessageBuilder.append("    ").append(commandName);
                if (!"help".equalsIgnoreCase(commandName)) {
                    helpMessageBuilder.append(" --help");
                }
                helpMessageBuilder.append("\n");
            }
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            LOG.error("Error in main", ex);
            RESULT_MANAGER.genericError(ex.getMessage());
        }

        return helpMessageBuilder.toString();
    }

    private SyncopeAdm() {
        // private constructor for static utility class
    }
}
