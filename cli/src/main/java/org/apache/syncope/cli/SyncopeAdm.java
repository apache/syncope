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
package org.apache.syncope.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.syncope.cli.commands.LoggerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeAdm {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeAdm.class);

    private static final String helpMessage = "Usage: Main [options]\n"
            + "  Options:\n"
            + "    logger --help \n";

    private static LoggerCommand loggerCommand;

    private static final JCommander jc = new JCommander();

    public static void main(final String[] args) {
        LOG.debug("Starting with args \n");

        for (final String arg : args) {
            LOG.debug("Arg: {}", arg);
        }

        instantiateCommands();

        if (args.length == 0) {
            System.out.println(helpMessage);
        } else {
            try {
                jc.parse(args);
            } catch (final ParameterException ioe) {
                System.out.println(helpMessage);
                LOG.error("Parameter exception", ioe);
            }
            executeCommand();
        }

    }

    private static void instantiateCommands() {
        LOG.debug("Init JCommander");
        loggerCommand = new LoggerCommand();
        jc.addCommand(loggerCommand);
        LOG.debug("Added LoggerCommand");
    }

    private static void executeCommand() {
        final String command = jc.getParsedCommand();

        LOG.debug("Called command {}", command);

        if ("logger".equalsIgnoreCase(command)) {
            loggerCommand.execute();
        }
    }
}
