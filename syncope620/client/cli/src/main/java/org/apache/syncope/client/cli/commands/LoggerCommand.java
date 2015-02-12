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
package org.apache.syncope.client.cli.commands;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(
        commandNames = "logger",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope logger service")
public class LoggerCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCommand.class);

    private final String helpMessage = "Usage: logger [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -r, --read \n"
            + "       Syntax: -r={LOG-NAME} \n"
            + "    -u, --update \n"
            + "       Syntax: {LOG-NAME}={LOG-LEVEL} \n"
            + "    -ua, --update-all \n"
            + "       Syntax: -ua={LOG-LEVEL} \n"
            + "    -c, --create \n"
            + "       Syntax: {LOG-NAME}={LOG-LEVEL} \n"
            + "    -d, --delete \n"
            + "       Syntax: -d={LOG-NAME}";

    @Parameter(names = { "-r", "--read" })
    public String logNameToRead;

    @DynamicParameter(names = { "-u", "--update" })
    private final Map<String, String> updateLogs = new HashMap<String, String>();

    @Parameter(names = { "-ua", "--update-all" })
    public String logLevel;

    @DynamicParameter(names = { "-c", "--create" })
    private final Map<String, String> createLogs = new HashMap<String, String>();

    @Parameter(names = { "-d", "--delete" })
    public String logNameToDelete;

    @Override
    public void execute() {
        final LoggerService loggerService = SyncopeServices.get(LoggerService.class);

        LOG.debug("Logger service successfully created");

        if (help) {
            LOG.debug("- logger help command");
            System.out.println(helpMessage);
        } else if (list) {
            LOG.debug("- logger list command");
            try {
                for (final LoggerTO loggerTO : loggerService.list(LoggerType.LOG)) {
                    System.out.println(" - " + loggerTO.getKey() + " -> " + loggerTO.getLevel());
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (StringUtils.isNotBlank(logNameToRead)) {
            LOG.debug("- logger read {} command", logNameToRead);
            try {
                final LoggerTO loggerTO = loggerService.read(LoggerType.LOG, logNameToRead);
                System.out.println(" - Logger " + loggerTO.getKey() + " with level -> " + loggerTO.getLevel());
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (!updateLogs.isEmpty()) {
            LOG.debug("- logger update command with params {}", updateLogs);

            for (final Map.Entry<String, String> log : updateLogs.entrySet()) {
                final LoggerTO loggerTO = loggerService.read(LoggerType.LOG, log.getKey());
                try {
                    loggerTO.setLevel(LoggerLevel.valueOf(log.getValue()));
                    loggerService.update(LoggerType.LOG, loggerTO.getKey(), loggerTO);
                    System.out.println(" - Logger " + loggerTO.getKey() + " new level -> " + loggerTO.getLevel());
                } catch (final SyncopeClientException ex) {
                    System.out.println(" - Error: " + ex.getMessage());
                } catch (final IllegalArgumentException ex) {
                    System.out.println(" - Error: " + log.getValue() + " isn't a valid logger level, try with:");
                    for (final LoggerLevel level : LoggerLevel.values()) {
                        System.out.println("  *** " + level.name());
                    }
                }
            }
        } else if (StringUtils.isNotBlank(logLevel)) {
            LOG.debug("- logger update all command with level {}", logLevel);
            for (final LoggerTO loggerTO : loggerService.list(LoggerType.LOG)) {
                try {
                    loggerTO.setLevel(LoggerLevel.valueOf(logLevel));
                    loggerService.update(LoggerType.LOG, loggerTO.getKey(), loggerTO);
                    System.out.println(" - Logger " + loggerTO.getKey() + " new level -> " + loggerTO.getLevel());
                } catch (final SyncopeClientException ex) {
                    System.out.println(" - Error: " + ex.getMessage());
                } catch (final IllegalArgumentException ex) {
                    System.out.println(" - Error: " + loggerTO.getLevel() + " isn't a valid logger level, try with:");
                    for (final LoggerLevel level : LoggerLevel.values()) {
                        System.out.println("  *** " + level.name());
                    }
                }
            }
        } else if (!createLogs.isEmpty()) {
            LOG.debug("- logger create command with params {}", createLogs);

            for (final Map.Entry<String, String> entrySet : createLogs.entrySet()) {
                final LoggerTO loggerTO = new LoggerTO();
                try {
                    loggerTO.setKey(entrySet.getKey());
                    loggerTO.setLevel(LoggerLevel.valueOf(entrySet.getValue()));
                    loggerService.update(LoggerType.LOG, loggerTO.getKey(), loggerTO);
                    System.out.println(" - Logger " + loggerTO.getKey() + " created with level -> " + loggerTO.
                            getLevel());
                } catch (final SyncopeClientException ex) {
                    System.out.println(" - Error: " + ex.getMessage());
                } catch (final IllegalArgumentException ex) {
                    System.out.println(" - Error: " + loggerTO.getLevel() + " isn't a valid logger level, try with:");
                    for (final LoggerLevel level : LoggerLevel.values()) {
                        System.out.println("  *** " + level.name());
                    }
                }
            }
        } else if (StringUtils.isNotBlank(logNameToDelete)) {
            try {
                LOG.debug("- logger delete {} command", logNameToDelete);
                loggerService.delete(LoggerType.LOG, logNameToDelete);
                System.out.println(" - Logger " + logNameToDelete + " deleted!");
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else {
            System.out.println(helpMessage);
        }
    }

}
