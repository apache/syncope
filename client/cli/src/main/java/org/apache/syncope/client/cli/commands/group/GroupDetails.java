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
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.GroupTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupDetails extends AbstractGroupCommand {

    private static final Logger LOG = LoggerFactory.getLogger(GroupDetails.class);

    private static final String DETAILS_HELP_MESSAGE = "group --details";

    private final Input input;

    public GroupDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedMap<>();
                final List<GroupTO> groupTOs = groupSyncopeOperations.list();
                int withoudResources = 0;
                int withoudAttributes = 0;
                int onRootRealm = 0;
                for (final GroupTO groupTO : groupTOs) {
                    if (groupTO.getResources() == null || groupTO.getResources().isEmpty()) {
                        withoudResources++;
                    }
                    if ((groupTO.getPlainAttrs() == null || groupTO.getPlainAttrs().isEmpty())
                            && (groupTO.getDerAttrs() == null || groupTO.getDerAttrs().isEmpty())
                            && (groupTO.getVirAttrs() == null || groupTO.getVirAttrs().isEmpty())) {
                        withoudAttributes++;
                    }
                    if (SyncopeConstants.ROOT_REALM.equals(groupTO.getRealm())) {
                        onRootRealm++;
                    }
                }
                details.put("Total number", String.valueOf(groupTOs.size()));
                details.put("Without resources", String.valueOf(withoudResources));
                details.put("Without attributes", String.valueOf(withoudAttributes));
                details.put("On root realm", String.valueOf(onRootRealm));
                details.put("On the other realm", String.valueOf(groupTOs.size() - onRootRealm));
                groupResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about realm", ex);
                groupResultManager.genericError(ex.getMessage());
            }
        } else {
            groupResultManager.unnecessaryParameters(input.listParameters(), DETAILS_HELP_MESSAGE);
        }
    }
}
