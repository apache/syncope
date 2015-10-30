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

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorDetails extends AbstractConnectorCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorDetails.class);

    private static final String DETAILS_HELP_MESSAGE = "connector --details";

    private final Input input;

    public ConnectorDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedMap<>();
                final List<ConnInstanceTO> connInstanceTOs = connectorSyncopeOperations.list();
                int withCreateCapability = 0;
                int withDeleteCapability = 0;
                int withSearchCapability = 0;
                int withSyncCapability = 0;
                int withUpdateCapability = 0;
                for (final ConnInstanceTO connInstanceTO : connInstanceTOs) {
                    if (connInstanceTO.getCapabilities().contains(ConnectorCapability.CREATE)) {
                        withCreateCapability++;
                    }
                    if (connInstanceTO.getCapabilities().contains(ConnectorCapability.DELETE)) {
                        withDeleteCapability++;
                    }
                    if (connInstanceTO.getCapabilities().contains(ConnectorCapability.SEARCH)) {
                        withSearchCapability++;
                    }
                    if (connInstanceTO.getCapabilities().contains(ConnectorCapability.SYNC)) {
                        withSyncCapability++;
                    }
                    if (connInstanceTO.getCapabilities().contains(ConnectorCapability.UPDATE)) {
                        withUpdateCapability++;
                    }
                }
                details.put("Total number", String.valueOf(connInstanceTOs.size()));
                details.put("With create capability", String.valueOf(withCreateCapability));
                details.put("With delete capability", String.valueOf(withDeleteCapability));
                details.put("With search capability", String.valueOf(withSearchCapability));
                details.put("With sync capability", String.valueOf(withSyncCapability));
                details.put("With update capability", String.valueOf(withUpdateCapability));
                details.put("Bundles number", String.valueOf(connectorSyncopeOperations.getBundles().size()));
                connectorResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about connector", ex);
                connectorResultManager.genericError(ex.getMessage());
            }
        } else {
            connectorResultManager.unnecessaryParameters(input.listParameters(), DETAILS_HELP_MESSAGE);
        }
    }
}
