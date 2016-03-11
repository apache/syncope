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
package org.apache.syncope.client.cli.commands.policy;

import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.types.PolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyDetails extends AbstractPolicyCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyDetails.class);

    private static final String DETAILS_HELP_MESSAGE = "policy --details";

    private final Input input;

    public PolicyDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedMap<>();
                final int accountPolicySize = policySyncopeOperations.list(PolicyType.ACCOUNT.name()).size();
                final int passwordPolicySize = policySyncopeOperations.list(PolicyType.PASSWORD.name()).size();
                final int pullPolicySize = policySyncopeOperations.list(PolicyType.PULL.name()).size();
                final int pushPolicySize = policySyncopeOperations.list(PolicyType.PUSH.name()).size();
                details.put("total number", String.valueOf(accountPolicySize
                        + passwordPolicySize
                        + pullPolicySize
                        + pushPolicySize));
                details.put("account policies", String.valueOf(accountPolicySize));
                details.put("password policies", String.valueOf(passwordPolicySize));
                details.put("pull policies", String.valueOf(pullPolicySize));
                details.put("push policies", String.valueOf(pushPolicySize));
                policyResultManager.printDetails(details);
            } catch (final Exception ex) {
                LOG.error("Error reading details about policy", ex);
                policyResultManager.genericError(ex.getMessage());
            }
        } else {
            policyResultManager.commandOptionError(DETAILS_HELP_MESSAGE);
        }
    }
}
