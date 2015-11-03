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
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.SyncPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;

public class PolicyResultManager extends CommonsResultManager {

    public void printPolicies(final LinkedList<AbstractPolicyTO> policyTOs) {
        System.out.println("");
        for (AbstractPolicyTO policyTO : policyTOs) {
            if (!policyTOs.isEmpty()) {
                final PolicyType policyType = policyTO.getType();
                switch (policyType) {
                    case ACCOUNT:
                        printAccountPolicy((AccountPolicyTO) policyTO);
                        break;
                    case PASSWORD:
                        printPasswordPolicy((PasswordPolicyTO) policyTO);
                        break;
                    case PUSH:
                        System.out.println(policyTO);
                        break;
                    case SYNC:
                        printSyncPolicy((SyncPolicyTO) policyTO);
                        break;
                    default:
                        break;
                }
            }
        }

    }

    public void printPoliciesByType(final String policyTypeString, final LinkedList<AbstractPolicyTO> policyTOs) {
        System.out.println("");
        final PolicyType policyType = PolicyType.valueOf(policyTypeString);
        switch (policyType) {
            case ACCOUNT:
                for (final AbstractPolicyTO policyTO : policyTOs) {
                    printAccountPolicy((AccountPolicyTO) policyTO);
                }
                break;
            case PASSWORD:
                for (final AbstractPolicyTO policyTO : policyTOs) {
                    printPasswordPolicy((PasswordPolicyTO) policyTO);
                }
                break;
            case PUSH:
                for (final AbstractPolicyTO policyTO : policyTOs) {
                    System.out.println(policyTO);
                }
                break;
            case SYNC:
                for (final AbstractPolicyTO policyTO : policyTOs) {
                    printSyncPolicy((SyncPolicyTO) policyTO);
                }
                break;
            default:
                break;
        }
    }

    public void printAccountPolicy(final AccountPolicyTO policyTO) {
        System.out.println(" > KEY: " + policyTO.getKey());
        System.out.println("    type: " + policyTO.getType().name());
        System.out.println("    description: " + policyTO.getDescription());
        System.out.println("    resources : " + policyTO.getUsedByResources().toString());
        System.out.println("    realms : " + policyTO.getUsedByRealms().toString());
        System.out.println("    max authentication attempts : " + policyTO.getMaxAuthenticationAttempts());
        System.out.println("    propagation suspension : " + policyTO.isPropagateSuspension());
        System.out.println("    RULES : ");
        System.out.println("       > class : " + policyTO.getRuleConfs());
        System.out.println("");
    }

    public void printPasswordPolicy(final PasswordPolicyTO policyTO) {
        System.out.println(" > KEY: " + policyTO.getKey());
        System.out.println("    type: " + policyTO.getType().name());
        System.out.println("    description: " + policyTO.getDescription());
        System.out.println("    resources : " + policyTO.getUsedByResources().toString());
        System.out.println("    realms : " + policyTO.getUsedByRealms().toString());
        System.out.println("    history lenght : " + policyTO.getHistoryLength());
        System.out.println("    allow null password : " + policyTO.isAllowNullPassword());
        System.out.println("    RULES : ");
        System.out.println("       > class : " + ((PasswordPolicyTO) policyTO).getRuleConfs());
        System.out.println("");
    }

    public void printSyncPolicy(final SyncPolicyTO policyTO) {
        System.out.println(" > KEY: " + policyTO.getKey());
        System.out.println("    type: " + policyTO.getType().name());
        System.out.println("    description: " + policyTO.getDescription());
        System.out.println("    resources : " + policyTO.getUsedByResources().toString());
        System.out.println("    realms : " + policyTO.getUsedByRealms().toString());
        if (policyTO.getSpecification() != null) {
            System.out.println("    conflict resolution action: "
                    + policyTO.getSpecification().getConflictResolutionAction().name());
            System.out.println("    correlation rule : "
                    + policyTO.getSpecification().getCorrelationRules().toString());
        }
        System.out.println("");
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("policies details", details);
    }
}
