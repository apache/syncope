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
package org.apache.syncope.core.rest.data;

import static org.apache.syncope.common.types.PolicyType.GLOBAL_ACCOUNT;
import static org.apache.syncope.common.types.PolicyType.GLOBAL_PASSWORD;
import static org.apache.syncope.common.types.PolicyType.GLOBAL_SYNC;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.springframework.stereotype.Component;

@Component
public class PolicyDataBinder {

    private boolean isGlobalPolicy(final PolicyType policyType) {
        boolean isGlobal;
        switch (policyType) {
            case GLOBAL_PASSWORD:
            case GLOBAL_ACCOUNT:
            case GLOBAL_SYNC:
                isGlobal = true;
                break;

            default:
                isGlobal = false;
        }

        return isGlobal;
    }

    /**
     * Get policy TO from policy bean.
     *
     * @param policy bean.
     * @return policy TO.
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicyTO> T getPolicyTO(final Policy policy) {
        final AbstractPolicyTO policyTO;

        final boolean isGlobal = isGlobalPolicy(policy.getType());

        switch (policy.getType()) {
            case GLOBAL_PASSWORD:
            case PASSWORD:
                if (!(policy.getSpecification() instanceof PasswordPolicySpec)) {
                    throw new ClassCastException("Expected " + PasswordPolicySpec.class.getName()
                            + ", found " + policy.getSpecification().getClass().getName());
                }
                policyTO = new PasswordPolicyTO(isGlobal);
                ((PasswordPolicyTO) policyTO).setSpecification((PasswordPolicySpec) policy.getSpecification());
                break;

            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                if (!(policy.getSpecification() instanceof AccountPolicySpec)) {
                    throw new ClassCastException("Expected " + AccountPolicySpec.class.getName()
                            + ", found " + policy.getSpecification().getClass().getName());
                }
                policyTO = new AccountPolicyTO(isGlobal);
                ((AccountPolicyTO) policyTO).setSpecification((AccountPolicySpec) policy.getSpecification());
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                if (!(policy.getSpecification() instanceof SyncPolicySpec)) {
                    throw new ClassCastException("Expected " + SyncPolicySpec.class.getName()
                            + ", found " + policy.getSpecification().getClass().getName());

                }
                policyTO = new SyncPolicyTO(isGlobal);
                ((SyncPolicyTO) policyTO).setSpecification((SyncPolicySpec) policy.getSpecification());
        }

        policyTO.setId(policy.getId());
        policyTO.setDescription(policy.getDescription());

        return (T) policyTO;
    }

    @SuppressWarnings("unchecked")
    public <T extends Policy> T getPolicy(T policy, final AbstractPolicyTO policyTO) {
        if (policy != null && policy.getType() != policyTO.getType()) {
            throw new IllegalArgumentException(
                    String.format("Cannot update %s from %s", policy.getType(), policyTO.getType()));
        }

        final boolean isGlobal = isGlobalPolicy(policyTO.getType());

        switch (policyTO.getType()) {
            case GLOBAL_PASSWORD:
            case PASSWORD:
                if (!(policyTO instanceof PasswordPolicyTO)) {
                    throw new ClassCastException("Expected " + PasswordPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) new PasswordPolicy(isGlobal);
                }
                policy.setSpecification(((PasswordPolicyTO) policyTO).getSpecification());
                break;

            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                if (!(policyTO instanceof AccountPolicyTO)) {
                    throw new ClassCastException("Expected " + AccountPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) new AccountPolicy(isGlobal);
                }
                policy.setSpecification(((AccountPolicyTO) policyTO).getSpecification());
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                if (!(policyTO instanceof SyncPolicyTO)) {
                    throw new ClassCastException("Expected " + SyncPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) new SyncPolicy(isGlobal);
                }
                policy.setSpecification(((SyncPolicyTO) policyTO).getSpecification());
        }

        policy.setDescription(policyTO.getDescription());

        return policy;
    }
}
