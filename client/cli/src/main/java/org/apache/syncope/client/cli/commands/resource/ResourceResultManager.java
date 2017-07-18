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
package org.apache.syncope.client.cli.commands.resource;

import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;

public class ResourceResultManager extends CommonsResultManager {

    public void printResources(final List<ResourceTO> resourceTOs) {
        System.out.println("");
        for (final ResourceTO resourceTO : resourceTOs) {
            printResource(resourceTO);
        }
    }

    private void printResource(final ResourceTO resourceTO) {
        System.out.println(" > RESOURCE KEY: " + resourceTO.getKey());
        System.out.println("    connector dispaly name: " + resourceTO.getConnectorDisplayName());
        System.out.println("    connector key: " + resourceTO.getConnector());
        System.out.println("    account policy key: " + resourceTO.getAccountPolicy());
        System.out.println("    password policy key: " + resourceTO.getPasswordPolicy());
        System.out.println("    pull policy key: " + resourceTO.getPullPolicy());
        System.out.println("    propagation actions class: " + resourceTO.getPropagationActionsClassNames());
        System.out.println("    propagation priority: " + resourceTO.getPropagationPriority());
        System.out.println("    PROVISIONS:");
        printProvision(resourceTO.getProvisions());
        System.out.println("    create trace level: " + resourceTO.getCreateTraceLevel());
        System.out.println("    delete trace level: " + resourceTO.getDeleteTraceLevel());
        System.out.println("    update trace level: " + resourceTO.getUpdateTraceLevel());
        System.out.println("    pull trace level: " + resourceTO.getProvisioningTraceLevel());
        System.out.println("    enforce mandatory condition: " + resourceTO.isEnforceMandatoryCondition());
        System.out.println("    override capabilities: " + resourceTO.isOverrideCapabilities());
        System.out.println("    random password if not provided: " + resourceTO.isRandomPwdIfNotProvided());
        System.out.println("");
    }

    private void printProvision(final List<ProvisionTO> provisionTOs) {
        for (final ProvisionTO provisionTO : provisionTOs) {
            System.out.println("       provision key: " + provisionTO.getKey());
            System.out.println("       any type: " + provisionTO.getAnyType());
            System.out.println("       object class: " + provisionTO.getObjectClass());
            System.out.println("       sync token: " + provisionTO.getSyncToken());
            System.out.println("       virtual schema: " + provisionTO.getVirSchemas());
            System.out.println("       MAPPING: ");
            printMapping(provisionTO.getMapping());
        }
    }

    private void printMapping(final MappingTO mappingTO) {
        System.out.println("          ConnObjectLink: " + mappingTO.getConnObjectLink());
        System.out.println("          MAPPING ITEM: ");
        printMappingItem(mappingTO.getItems());
    }

    private void printMappingItem(final List<ItemTO> mappingItemTOs) {
        for (final ItemTO mappingItemTO : mappingItemTOs) {
            System.out.println("             mapping key: " + mappingItemTO.getKey());
            System.out.println("             internal attribute name: " + mappingItemTO.getIntAttrName());
            System.out.println("             external attribute name: " + mappingItemTO.getExtAttrName());
            System.out.println("             mandatory condition: " + mappingItemTO.getMandatoryCondition());
            System.out.println("             JEXL propagation transformer: "
                    + mappingItemTO.getPropagationJEXLTransformer());
            System.out.println("             JEXL pull transformer: "
                    + mappingItemTO.getPullJEXLTransformer());
            System.out.println("             transformers classes: "
                    + mappingItemTO.getTransformerClassNames());
            System.out.println("             purpose: " + mappingItemTO.getPurpose());
            System.out.println("             connector object key: " + mappingItemTO.isConnObjectKey());
            System.out.println("             password: " + mappingItemTO.isPassword());
            System.out.println("");
        }
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("resources details", details);
    }
}
