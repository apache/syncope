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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;

public class ConnectorResultManager extends CommonsResultManager {

    public void printConnectors(final List<ConnInstanceTO> connInstanceTOs) {
        System.out.println("");
        for (final ConnInstanceTO connInstanceTO : connInstanceTOs) {
            printConnector(connInstanceTO);
        }
    }

    public void printConnector(final ConnInstanceTO connInstanceTO) {
        System.out.println(" > CONNECTOR KEY: " + connInstanceTO.getKey());
        System.out.println("    bundle name: " + connInstanceTO.getBundleName());
        System.out.println("    connector name: " + connInstanceTO.getConnectorName());
        System.out.println("    display name: " + connInstanceTO.getDisplayName());
        System.out.println("    location: " + connInstanceTO.getLocation());
        System.out.println("    version: " + connInstanceTO.getVersion());
        System.out.println("    timeout: " + connInstanceTO.getConnRequestTimeout());
        System.out.println("    CAPABILITIES:");
        printCapabilities(connInstanceTO.getCapabilities());
        System.out.println("    CONFIGURATION:");
        printConfiguration(connInstanceTO.getConf());
        System.out.println("    POOL CONFIGURATION:");
        printConfPool(connInstanceTO.getPoolConf());
        System.out.println("");
    }

    private void printCapabilities(final Set<ConnectorCapability> capabilities) {
        for (final ConnectorCapability capability : capabilities) {
            System.out.println("       - " + capability.name());
        }
    }

    private void printConfPool(final ConnPoolConfTO connPoolConfTO) {
        System.out.println("       min idle: " + connPoolConfTO.getMinIdle());
        System.out.println("       min evictlable idle: " + connPoolConfTO.getMinEvictableIdleTimeMillis());
        System.out.println("       max idle: " + connPoolConfTO.getMaxIdle());
        System.out.println("       max objects: " + connPoolConfTO.getMaxObjects());
        System.out.println("       max wait: " + connPoolConfTO.getMaxWait());
    }

    public void printBundles(final List<ConnBundleTO> connBundleTOs) {
        for (final ConnBundleTO connBundleTO : connBundleTOs) {
            System.out.println(" > BUNDLE NAME: " + connBundleTO.getBundleName());
            System.out.println("    connector name: " + connBundleTO.getConnectorName());
            System.out.println("    display name: " + connBundleTO.getDisplayName());
            System.out.println("    location: " + connBundleTO.getLocation());
            System.out.println("    version: " + connBundleTO.getVersion());
        }
    }

    private void printConfPropSchema(final List<ConnConfPropSchema> connConfPropSchemas) {
        for (final ConnConfPropSchema connConfPropSchema : connConfPropSchemas) {
            System.out.println("       name: " + connConfPropSchema.getName());
            System.out.println("       display name: " + connConfPropSchema.getDisplayName());
            System.out.println("       help message: " + connConfPropSchema.getHelpMessage());
            System.out.println("       type: " + connConfPropSchema.getType());
            System.out.println("       order: " + connConfPropSchema.getOrder());
            System.out.println("       default value: " + connConfPropSchema.getDefaultValues().toString());
            System.out.println("");
        }
    }

    public void printConfigurationProperties(final Collection<ConnConfProperty> connConfPropertys) {
        printConfiguration(connConfPropertys);

    }

    public void printDetails(final Map<String, String> details) {
        printDetails("connectors details", details);
    }
}
