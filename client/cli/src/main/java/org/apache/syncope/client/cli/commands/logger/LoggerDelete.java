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

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerDelete extends AbstractLoggerCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerDelete.class);

    private static final String DELETE_HELP_MESSAGE = "logger --delete {LOG-NAME} {LOG-NAME} [...]";

    private final Input input;

    public LoggerDelete(final Input input) {
        this.input = input;
    }

    public void delete() {
        if (input.parameterNumber() >= 1) {
            for (final String parameter : input.getParameters()) {
                try {
                    loggerSyncopeOperations.delete(parameter);
                    loggerResultManager.deletedMessage("Logger", parameter);
                } catch (final WebServiceException | SyncopeClientException ex) {
                    LOG.error("Error deleting logger", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        loggerResultManager.notFoundError("Logger", parameter);
                    } else {
                        loggerResultManager.genericError(ex.getMessage());
                    }
                }
            }
        } else {
            loggerResultManager.commandOptionError(DELETE_HELP_MESSAGE);
        }
    }
}
