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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.syncope.client.cli.commands.ConfigurationCommand;
import org.apache.syncope.client.cli.commands.LoggerCommand;
import org.apache.syncope.client.cli.commands.NotificationCommand;
import org.apache.syncope.client.cli.commands.PolicyCommand;
import org.apache.syncope.client.cli.commands.ReportCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SyncopeAdm {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeAdm.class);

    private static final String HELP_MESSAGE = "Usage: Main [options]\n"
            + "  Options:\n"
            + "    logger --help \n"
            + "    config --help \n"
            + "    notification --help \n"
            + "    report --help \n"
            + "    policy --help \n"
            + "    entitlement --help \n";

    private static final JCommander JCOMMANDER = new JCommander();

    private static LoggerCommand LOGGER_COMMAND;

    private static ConfigurationCommand CONFIGURATION_COMMAND;

    private static NotificationCommand NOTIFICATION_COMMAND;

    private static ReportCommand REPORT_COMMAND;

    private static PolicyCommand POLICY_COMMAND;

    public static void main(final String[] args) {
        LOG.debug("Starting with args \n");

        for (final String arg : args) {
            LOG.debug("Arg: {}", arg);
        }

        instantiateCommands();

        if (args.length == 0) {
            System.out.println(HELP_MESSAGE);
        } else {
            try {
                JCOMMANDER.parse(args);
            } catch (final ParameterException ioe) {
                System.out.println(HELP_MESSAGE);
                LOG.error("Parameter exception", ioe);
            }
            executeCommand();
        }

    }

    private static void instantiateCommands() {
        LOG.debug("Init JCommander");
        LOGGER_COMMAND = new LoggerCommand();
        JCOMMANDER.addCommand(LOGGER_COMMAND);
        LOG.debug("Added LoggerCommand");
        CONFIGURATION_COMMAND = new ConfigurationCommand();
        JCOMMANDER.addCommand(CONFIGURATION_COMMAND);
        LOG.debug("Added ConfigurationCommand");
        NOTIFICATION_COMMAND = new NotificationCommand();
        JCOMMANDER.addCommand(NOTIFICATION_COMMAND);
        LOG.debug("Added NotificationCommand");
        REPORT_COMMAND = new ReportCommand();
        JCOMMANDER.addCommand(REPORT_COMMAND);
        LOG.debug("Added ReportCommand");
        POLICY_COMMAND = new PolicyCommand();
        JCOMMANDER.addCommand(POLICY_COMMAND);
        LOG.debug("Added PolicyCommand");
    }

    private static void executeCommand() {
        final String command = JCOMMANDER.getParsedCommand();

        LOG.debug("Called command {}", command);

        if ("logger".equalsIgnoreCase(command)) {
            LOGGER_COMMAND.execute();
        } else if ("config".equalsIgnoreCase(command)) {
            CONFIGURATION_COMMAND.execute();
        } else if ("notification".equalsIgnoreCase(command)) {
            NOTIFICATION_COMMAND.execute();
        } else if ("report".equalsIgnoreCase(command)) {
            REPORT_COMMAND.execute();
        } else if ("policy".equalsIgnoreCase(command)) {
            POLICY_COMMAND.execute();
        }
    }

    private SyncopeAdm() {
        // private constructor for static utility class
    }
}
