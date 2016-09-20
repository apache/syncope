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
package org.apache.syncope.client.cli.commands.group;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;

public class GroupResultManager extends CommonsResultManager {

    public void printGroups(final List<GroupTO> groupTOs) {
        System.out.println("");
        for (final GroupTO groupTO : groupTOs) {
            printGroup(groupTO);
        }
    }

    public void printGroup(final GroupTO groupTO) {
        System.out.println(" > GROUP KEY: " + groupTO.getKey());
        System.out.println("    name: " + groupTO.getName());
        System.out.println("    type: " + groupTO.getType());
        System.out.println("    realm: " + groupTO.getRealm());
        System.out.println("    status: " + groupTO.getStatus());
        System.out.println("    user owner: " + groupTO.getUserOwner());
        System.out.println("    group owner: " + groupTO.getGroupOwner());
        System.out.println("    RESOURCES: ");
        printResources(groupTO.getResources());
        System.out.println("    PLAIN ATTRIBUTES: ");
        printAttributes(groupTO.getPlainAttrs());
        System.out.println("    DERIVED ATTRIBUTES: ");
        printAttributes(groupTO.getDerAttrs());
        System.out.println("    VIRTUAL ATTRIBUTES: ");
        printAttributes(groupTO.getVirAttrs());
        System.out.println("");
    }

    private void printResources(final Set<String> resources) {
        for (final String resource : resources) {
            System.out.println("      - " + resource);
        }
    }

    public void printAttributes(final Set<AttrTO> attributes) {
        for (final AttrTO attribute : attributes) {
            printAttribute(attribute);
        }
    }

    public void printAttribute(final AttrTO attribute) {
        final StringBuilder attributeMessageBuilder = new StringBuilder();
        attributeMessageBuilder.append("     - ")
                .append(attribute.getSchema())
                .append(": ")
                .append(attribute.getValues());
        System.out.println(attributeMessageBuilder.toString());
        System.out.println("");
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("groups details", details);
    }
}
