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

import java.util.LinkedList;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;

public class PolicyList extends AbstractPolicyCommand {

    private static final String LIST_HELP_MESSAGE = "policy --list-policy {POLICY-TYPE}\n"
            + "   Policy type: ACCOUNT / PASSWORD / SYNC / PUSH";

    private final Input input;

    public PolicyList(final Input input) {
        this.input = input;
    }

    public void list() {

        if (input.parameterNumber() == 1) {
            try {
                final PolicyType policyType = PolicyType.valueOf(input.firstParameter());
                final LinkedList<AbstractPolicyTO> policyTOs = new LinkedList<>();
                for (final AbstractPolicyTO policyTO : policySyncopeOperations.list(policyType)) {
                    policyTOs.add(policyTO);
                }
                policyResultManager.fromList(policyType, policyTOs);
            } catch (final SyncopeClientException ex) {
                policyResultManager.genericError(ex.getMessage());
            } catch (final IllegalArgumentException ex) {
                policyResultManager.typeNotValidError(
                        "policy", input.firstParameter(), CommandUtils.fromEnumToArray(PolicyType.class));
            }
        } else {
            policyResultManager.commandOptionError(LIST_HELP_MESSAGE);
        }
    }
}
