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
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;

public class LoggerCreate extends AbstractLoggerCommand {

    private static final String CREATE_HELP_MESSAGE
            = "logger --create {LOG-NAME}={LOG-LEVEL} {LOG-NAME}={LOG-LEVEL} [...]";

    private final Input input;

    public LoggerCreate(final Input input) {
        this.input = input;
    }

    public void create() {
        if (input.parameterNumber() >= 1) {
            Input.PairParameter pairParameter;
            LoggerTO loggerTO;
            final LinkedList<LoggerTO> loggerTOs = new LinkedList<>();
            boolean failed = false;
            for (final String parameter : input.getParameters()) {
                loggerTO = new LoggerTO();
                try {
                    pairParameter = input.toPairParameter(parameter);
                    loggerTO.setKey(pairParameter.getKey());
                    loggerTO.setLevel(LoggerLevel.valueOf(pairParameter.getValue()));
                    loggerService.update(LoggerType.LOG, loggerTO);
                    loggerTOs.add(loggerTO);
                } catch (final WebServiceException | SyncopeClientException | IllegalArgumentException ex) {
                    Messages.printTypeNotValidMessage(
                            "logger level", input.firstParameter(), CommandUtils.fromEnumToArray(LoggerLevel.class));
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                resultManager.fromUpdate(loggerTOs);
            }
        } else {
            Messages.printCommandOptionMessage(CREATE_HELP_MESSAGE);
        }
    }

}
