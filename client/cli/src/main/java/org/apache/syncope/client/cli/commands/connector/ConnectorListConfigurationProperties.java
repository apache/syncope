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

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorListConfigurationProperties extends AbstractConnectorCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorListConfigurationProperties.class);

    private static final String LIST_CONFIGURATION_HELP_MESSAGE
            = "connector --list-configuration-properties {CONNECTOR-KEY} {CONNECTOR-KEY} [...]";

    private final Input input;

    public ConnectorListConfigurationProperties(final Input input) {
        this.input = input;
    }

    public void list() {
        if (input.getParameters().length >= 1) {
            for (final String parameter : input.getParameters()) {
                try {
                    connectorResultManager.printConfigurationProperties(
                            connectorSyncopeOperations.read(parameter).getConf());
                } catch (final NumberFormatException ex) {
                    LOG.error("Error listening connector", ex);
                    connectorResultManager.numberFormatException("connector", parameter);
                } catch (final SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error listening connector", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        connectorResultManager.notFoundError("Connector", parameter);
                    } else {
                        connectorResultManager.genericError(ex.getMessage());
                    }
                }
            }
        } else {
            connectorResultManager.commandOptionError(LIST_CONFIGURATION_HELP_MESSAGE);
        }
    }
}
