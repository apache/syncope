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
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerRead extends AbstractLoggerCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerRead.class);

    private static final String READ_HELP_MESSAGE = "logger --read {LOG-NAME} {LOG-NAME} [...]";

    private final Input input;

    public LoggerRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.getParameters().length >= 1) {
            final List<LoggerTO> loggerTOs = new ArrayList<>();
            boolean failed = false;
            for (final String parameter : input.getParameters()) {
                try {
                    loggerTOs.add(loggerSyncopeOperations.read(parameter));
                } catch (final SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error reading logger", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        loggerResultManager.notFoundError("Logger", parameter);
                    } else {
                        loggerResultManager.genericError(ex.getMessage());
                    }
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                loggerResultManager.fromRead(loggerTOs);
            }
        } else {
            loggerResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
