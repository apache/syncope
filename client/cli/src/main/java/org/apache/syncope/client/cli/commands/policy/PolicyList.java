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

import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.PolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyList extends AbstractPolicyCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyList.class);

    private static final String LIST_HELP_MESSAGE = "policy --list {POLICY-TYPE}\n"
            + "   Policy type: ACCOUNT / PASSWORD / SYNC / PUSH";

    private final Input input;

    public PolicyList(final Input input) {
        this.input = input;
    }

    public void list() {
        if (input.parameterNumber() == 1) {
            try {
                policyResultManager.printPoliciesByType(
                        input.firstParameter(), policySyncopeOperations.list(input.firstParameter()));
            } catch (final SyncopeClientException ex) {
                LOG.error("Error listing policy", ex);
                policyResultManager.genericError(ex.getMessage());
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error listing policy", ex);
                policyResultManager.typeNotValidError(
                        "policy", input.firstParameter(), CommandUtils.fromEnumToArray(PolicyType.class));
            }
        } else {
            policyResultManager.commandOptionError(LIST_HELP_MESSAGE);
        }
    }
}
