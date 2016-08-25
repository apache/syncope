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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorDelete extends AbstractConnectorCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorDelete.class);

    private static final String DELETE_HELP_MESSAGE = "connector --delete {CONNECTOR-KEY} {CONNECTOR-KEY} [...]";

    private final Input input;

    public ConnectorDelete(final Input input) {
        this.input = input;
    }

    public void delete() {
        if (input.getParameters().length >= 1) {
            final List<ConnInstanceTO> connInstanceTOs = new ArrayList<>();
            for (final String parameter : input.getParameters()) {
                try {
                    connectorSyncopeOperations.delete(parameter);
                    connectorResultManager.deletedMessage("connector", parameter);
                } catch (final NumberFormatException ex) {
                    connectorResultManager.numberFormatException("connector", parameter);
                } catch (final SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error deleting connector", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        LOG.error("Error deleting connector", ex);
                        connectorResultManager.notFoundError("Connector", parameter);
                    } else {
                        connectorResultManager.genericError(ex.getMessage());
                    }
                    break;
                }
            }
            connectorResultManager.printConnectors(connInstanceTOs);
        } else {
            connectorResultManager.commandOptionError(DELETE_HELP_MESSAGE);
        }
    }
}
