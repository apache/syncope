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

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUpdate extends AbstractLoggerCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerUpdate.class);

    private static final String UPDATE_HELP_MESSAGE =
            "logger --update {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]";

    private final Input input;

    public LoggerUpdate(final Input input) {
        this.input = input;
    }

    public void update() {
        if (input.parameterNumber() >= 1) {
            final List<LoggerTO> loggerTOs = new ArrayList<>();
            boolean failed = false;
            for (String parameter : input.getParameters()) {
                Pair<String, String> pairParameter = Input.toPairParameter(parameter);
                try {
                    LoggerTO loggerTO = loggerSyncopeOperations.read(pairParameter.getKey());
                    loggerTO.setLevel(LoggerLevel.valueOf(pairParameter.getValue()));
                    loggerSyncopeOperations.update(loggerTO);
                    loggerTOs.add(loggerTO);
                } catch (WebServiceException | SyncopeClientException | IllegalArgumentException ex) {
                    LOG.error("Error updating logger", ex);
                    if (ex.getMessage().startsWith("No enum constant org.apache.syncope.common.lib.types.")) {
                        loggerResultManager.typeNotValidError(
                                "logger level",
                                input.firstParameter(),
                                CommandUtils.fromEnumToArray(LoggerLevel.class));
                    } else if ("Parameter syntax error!".equalsIgnoreCase(ex.getMessage())) {
                        loggerResultManager.genericError(ex.getMessage());
                        loggerResultManager.genericError(UPDATE_HELP_MESSAGE);
                    } else if (ex.getMessage().startsWith("NotFound")) {
                        loggerResultManager.notFoundError("Logger", parameter);
                    } else {
                        loggerResultManager.genericError(ex.getMessage());
                        loggerResultManager.genericError(UPDATE_HELP_MESSAGE);
                    }
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                loggerResultManager.fromUpdate(loggerTOs);
            }
        } else {
            loggerResultManager.commandOptionError(UPDATE_HELP_MESSAGE);
        }
    }
}
