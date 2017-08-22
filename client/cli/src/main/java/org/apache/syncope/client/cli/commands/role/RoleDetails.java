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
package org.apache.syncope.client.cli.commands.role;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleDetails extends AbstractRoleCommand {

    private static final Logger LOG = LoggerFactory.getLogger(RoleDetails.class);

    private static final String DETAILS_HELP_MESSAGE = "role --details";

    private final Input input;

    public RoleDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedHashMap<>();
                final List<RoleTO> roleTOs = roleSyncopeOperations.list();
                int withoutEntitlements = 0;
                for (final RoleTO roleTO : roleTOs) {
                    if (roleTO.getEntitlements() == null || roleTO.getEntitlements().isEmpty()) {
                        withoutEntitlements++;
                    }
                }
                details.put("Total number", String.valueOf(roleTOs.size()));
                details.put("Without entitlements", String.valueOf(withoutEntitlements));
                roleResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about role", ex);
                roleResultManager.genericError(ex.getMessage());
            }
        } else {
            roleResultManager.unnecessaryParameters(input.listParameters(), DETAILS_HELP_MESSAGE);
        }
    }
}
