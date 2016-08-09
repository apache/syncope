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
package org.apache.syncope.client.cli.commands.logger;

import java.util.LinkedList;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerCreate extends AbstractLoggerCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCreate.class);

    private static final String CREATE_HELP_MESSAGE =
            "logger --create {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]";

    private final Input input;

    public LoggerCreate(final Input input) {
        this.input = input;
    }

    public void create() {
        if (input.parameterNumber() >= 1) {
            final LinkedList<LoggerTO> loggerTOs = new LinkedList<>();
            boolean failed = false;
            for (String parameter : input.getParameters()) {
                LoggerTO loggerTO = new LoggerTO();
                Pair<String, String> pairParameter = input.toPairParameter(parameter);
                try {
                    loggerTO.setKey(pairParameter.getKey());
                    loggerTO.setLevel(LoggerLevel.valueOf(pairParameter.getValue()));
                    loggerSyncopeOperations.update(loggerTO);
                    loggerTOs.add(loggerTO);
                } catch (WebServiceException | SyncopeClientException | IllegalArgumentException ex) {
                    LOG.error("Error creating logger", ex);
                    loggerResultManager.typeNotValidError(
                            "logger level", input.firstParameter(), CommandUtils.fromEnumToArray(LoggerLevel.class));
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                loggerResultManager.fromUpdate(loggerTOs);
            }
        } else {
            loggerResultManager.commandOptionError(CREATE_HELP_MESSAGE);
        }
    }
}
