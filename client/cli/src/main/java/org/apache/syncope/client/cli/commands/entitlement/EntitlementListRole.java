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
package org.apache.syncope.client.cli.commands.entitlement;

import java.util.Set;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntitlementListRole extends AbstractEntitlementCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EntitlementListRole.class);

    private static final String READ_HELP_MESSAGE = "entitlement --list-role {ENTITLEMENT-NAME}";

    private final Input input;

    public EntitlementListRole(final Input input) {
        this.input = input;
    }

    public void list() {
        if (input.getParameters().length == 1) {
            try {
                final Set<RoleTO> roleTOs = entitlementSyncopeOperations.rolePerEntitlements(input.firstParameter());
                if (!entitlementSyncopeOperations.exists(input.firstParameter())) {
                    entitlementResultManager.notFoundError("Entitlement", input.firstParameter());
                } else if (roleTOs != null && !roleTOs.isEmpty()) {
                    entitlementResultManager.rolesToView(roleTOs);
                } else {
                    entitlementResultManager.genericMessage("No roles found for entitlement " + input.firstParameter());
                }
            } catch (final SyncopeClientException | WebServiceException ex) {
                LOG.error("Error reading entitlement", ex);
                if (ex.getMessage().startsWith("NotFound")) {
                    entitlementResultManager.notFoundError("User", input.firstParameter());
                } else {
                    entitlementResultManager.genericError(ex.getMessage());
                }
            } catch (final NumberFormatException ex) {
                LOG.error("Error reading entitlement", ex);
                entitlementResultManager.numberFormatException("user", input.firstParameter());
            }
        } else {
            entitlementResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
