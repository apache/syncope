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
package org.apache.syncope.cli.commands;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.cli.SyncopeServices;
import org.apache.syncope.cli.validators.DebugLevelValidator;
import org.apache.syncope.common.services.LoggerService;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.types.LoggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(
        commandNames = "logger",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope logger service")
public class LoggerCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCommand.class);

    private static final Class syncopeLoggerClass = LoggerService.class;

    private final String helpMessage = "Usage: logger [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -ua, --update-all \n"
            + "       Syntax: -ua={LOGGER-LEVEL} \n"
            + "    -u, --update \n"
            + "       Syntax: {LOG-NAME}={LOG-LEVEL} \n"
            + "    -d, --delete \n"
            + "       Syntax: -d={LOGGER-NAME}";

    @Parameter(names = {"-h", "--help"})
    public boolean help = false;

    @Parameter(names = {"-l", "--list"})
    public boolean list = false;

    @Parameter(names = {"-ua", "--update-all"}, validateWith = DebugLevelValidator.class)
    public String logLevel;

    @Parameter(names = {"-r", "--read"})
    public String logNameToRead;

    @Parameter(names = {"-d", "--delete"})
    public String logNameToDelete;

    @DynamicParameter(names = {"-u", "--update"})
    private final Map<String, String> params = new HashMap<String, String>();

    public void execute() {
        final LoggerService loggerService = ((LoggerService) SyncopeServices.get(syncopeLoggerClass));

        LOG.debug("Logger service successfully created");

        if (help) {
            LOG.debug("- logger help command");
            System.out.println(helpMessage);
        } else if (list) {
            LOG.debug("- logger list command");
            for (final LoggerTO loggerTO : loggerService.list(LoggerType.LOG)) {
                System.out.println(" - " + loggerTO.getName() + " -> " + loggerTO.getLevel());
            }
        } else if (StringUtils.isNotBlank(logLevel)) {
            LOG.debug("- logger update all command with level {}", logLevel);
            for (final LoggerTO loggerTO : loggerService.list(LoggerType.LOG)) {
                loggerTO.setLevel(LoggerLevel.valueOf(logLevel));
                loggerService.update(LoggerType.LOG, loggerTO.getName(), loggerTO);
                System.out.println(" - Logger " + loggerTO.getName() + " new value -> " + loggerTO.getLevel());
            }
        } else if (!params.isEmpty()) {
            LOG.debug("- logger update command with params {}", params);
            for (final Map.Entry<String, String> entrySet : params.entrySet()) {
                final LoggerTO loggerTO = loggerService.read(LoggerType.LOG, entrySet.getKey());
                loggerTO.setLevel(LoggerLevel.valueOf(entrySet.getValue()));
                loggerService.update(LoggerType.LOG, loggerTO.getName(), loggerTO);
                System.out.println(" - Logger " + loggerTO.getName() + " new value -> " + loggerTO.getLevel());
            }
        } else if (StringUtils.isNotBlank(logNameToRead)) {
            LOG.debug("- logger read {} command", logNameToRead);
            final LoggerTO loggerTO = loggerService.read(LoggerType.LOG, logNameToRead);
            System.out.println(" - Logger " + loggerTO.getName() + " with level -> " + loggerTO.getLevel());
        } else if (StringUtils.isNotBlank(logNameToDelete)) {
            LOG.debug("- logger delete {} command", logNameToDelete);
            loggerService.delete(LoggerType.LOG, logNameToDelete);
            System.out.println(" - Logger " + logNameToDelete + " deleted!");
        } else {
            System.out.println(helpMessage);
        }
    }

}
