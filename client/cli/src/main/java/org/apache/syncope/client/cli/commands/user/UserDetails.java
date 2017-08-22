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
package org.apache.syncope.client.cli.commands.user;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDetails extends AbstractUserCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UserDetails.class);

    private static final String COUNT_HELP_MESSAGE = "user --details";

    private final Input input;

    public UserDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedHashMap<>();
                final List<UserTO> usersTOs = userSyncopeOperations.list().getResult();
                int withoutResource = 0;
                int withoutRole = 0;
                int activeStatus = 0;
                int suspendedStatus = 0;
                for (final UserTO userTO : usersTOs) {
                    if (userTO.getResources().isEmpty()) {
                        withoutResource++;
                    }
                    if (userTO.getRoles().isEmpty()) {
                        withoutRole++;
                    }
                    if ("active".equalsIgnoreCase(userTO.getStatus())) {
                        activeStatus++;
                    } else if ("suspended".equalsIgnoreCase(userTO.getStatus())) {
                        suspendedStatus++;
                    }
                }
                details.put("Total number", String.valueOf(usersTOs.size()));
                details.put("Active", String.valueOf(activeStatus));
                details.put("Suspended", String.valueOf(suspendedStatus));
                details.put("Without resources", String.valueOf(withoutResource));
                details.put("Without roles", String.valueOf(withoutRole));
                userResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about user", ex);
                userResultManager.genericError(ex.getMessage());
            }
        } else {
            userResultManager.unnecessaryParameters(input.listParameters(), COUNT_HELP_MESSAGE);
        }
    }
}
