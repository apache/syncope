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
package org.apache.syncope.client.cli.commands.connector;

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;

public class ConnectorRead extends AbstractConnectorCommand {

    private static final String READ_HELP_MESSAGE = "connector --read {CONNECTOR-ID} {CONNECTOR-ID} [...]";

    private final Input input;

    public ConnectorRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.getParameters().length >= 1) {
            final List<ConnInstanceTO> connInstanceTOs = new ArrayList<>();
            for (final String parameter : input.getParameters()) {
                try {
                    connInstanceTOs.add(connectorSyncopeOperations.read(parameter));
                } catch (final NumberFormatException ex) {
                    connectorResultManager.numberFormatException("connector", parameter);
                } catch (final SyncopeClientException | WebServiceException ex) {
                    if (ex.getMessage().startsWith("NotFound")) {
                        connectorResultManager.notFoundError("Connector", parameter);
                    } else {
                        connectorResultManager.generic(ex.getMessage());
                    }
                    break;
                }
            }
            connectorResultManager.toView(connInstanceTOs);
        } else {
            connectorResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }

}
