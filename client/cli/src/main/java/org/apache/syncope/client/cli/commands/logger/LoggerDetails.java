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

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerDetails extends AbstractLoggerCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerDetails.class);

    private static final String LIST_HELP_MESSAGE = "logger --details";

    private final Input input;

    public LoggerDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedMap<>();
                final List<LoggerTO> loggerTOs = loggerSyncopeOperations.list();
                int debugLevel = 0;
                int errorLevel = 0;
                int fatalLevel = 0;
                int infoLevel = 0;
                int offLevel = 0;
                int traceLevel = 0;
                int warnLevel = 0;
                for (final LoggerTO loggerTO : loggerTOs) {
                    switch (loggerTO.getLevel()) {
                        case DEBUG:
                            debugLevel++;
                            break;
                        case ERROR:
                            errorLevel++;
                            break;
                        case FATAL:
                            fatalLevel++;
                            break;
                        case INFO:
                            infoLevel++;
                            break;
                        case OFF:
                            offLevel++;
                            break;
                        case TRACE:
                            traceLevel++;
                            break;
                        case WARN:
                            warnLevel++;
                            break;
                        default:
                            break;
                    }
                }
                details.put("Total number", String.valueOf(loggerTOs.size()));
                details.put("Set to DEBUG", String.valueOf(debugLevel));
                details.put("Set to ERROR", String.valueOf(errorLevel));
                details.put("Set to FATAL", String.valueOf(fatalLevel));
                details.put("Set to INFO", String.valueOf(infoLevel));
                details.put("Set to OFF", String.valueOf(offLevel));
                details.put("Set to TRACE", String.valueOf(traceLevel));
                details.put("Set to WARN", String.valueOf(warnLevel));
                loggerResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about logger", ex);
                loggerResultManager.genericError(ex.getMessage());
            }
        } else {
            loggerResultManager.unnecessaryParameters(input.listParameters(), LIST_HELP_MESSAGE);
        }
    }
}
