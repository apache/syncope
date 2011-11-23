/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.data;

import org.springframework.stereotype.Component;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.types.AccountPolicySpec;
import org.syncope.types.PasswordPolicySpec;
import org.syncope.types.SyncPolicySpec;

@Component
public class PolicyDataBinder {

    /**
     * Get policy TO from policy bean.
     * @param policy bean.
     * @return policy TO.
     */
    public PolicyTO getPolicyTO(final Policy policy) {
        final PolicyTO policyTO;

        if (policy != null) {
            switch (policy.getType()) {
                case GLOBAL_PASSWORD:
                    policyTO = new PasswordPolicyTO(true);
                    ((PasswordPolicyTO) policyTO).setSpecification(
                            (PasswordPolicySpec) policy.getSpecification());
                    break;

                case PASSWORD:
                    policyTO = new PasswordPolicyTO();
                    ((PasswordPolicyTO) policyTO).setSpecification(
                            (PasswordPolicySpec) policy.getSpecification());
                    break;

                case GLOBAL_ACCOUNT:
                    policyTO = new AccountPolicyTO(true);
                    ((AccountPolicyTO) policyTO).setSpecification(
                            (AccountPolicySpec) policy.getSpecification());
                    break;

                case ACCOUNT:
                    policyTO = new AccountPolicyTO();
                    ((AccountPolicyTO) policyTO).setSpecification(
                            (AccountPolicySpec) policy.getSpecification());
                    break;

                case GLOBAL_SYNC:
                    policyTO = new SyncPolicyTO(true);
                    ((SyncPolicyTO) policyTO).setSpecification(
                            (SyncPolicySpec) policy.getSpecification());
                    break;

                case SYNC:
                default:
                    policyTO = new SyncPolicyTO();
                    ((SyncPolicyTO) policyTO).setSpecification(
                            (SyncPolicySpec) policy.getSpecification());

            }

            policyTO.setId(policy.getId());
            policyTO.setDescription(policy.getDescription());
        } else {
            policyTO = null;
        }

        return policyTO;
    }

    /**
     * Get policy bean from policy TO.
     * @param policyTO TO
     * @return policy bean
     */
    public Policy getPolicy(final PolicyTO policyTO) {
        final Policy policy;

        if (policyTO != null) {
            switch (policyTO.getType()) {
                case GLOBAL_PASSWORD:
                    policy = new PasswordPolicy(true);
                    policy.setSpecification(
                            ((PasswordPolicyTO) policyTO).getSpecification());
                    break;

                case PASSWORD:
                    policy = new PasswordPolicy();
                    policy.setSpecification(
                            ((PasswordPolicyTO) policyTO).getSpecification());
                    break;

                case GLOBAL_ACCOUNT:
                    policy = new AccountPolicy(true);
                    policy.setSpecification(
                            ((AccountPolicyTO) policyTO).getSpecification());
                    break;

                case ACCOUNT:
                    policy = new AccountPolicy();
                    policy.setSpecification(
                            ((AccountPolicyTO) policyTO).getSpecification());
                    break;

                case GLOBAL_SYNC:
                    policy = new SyncPolicy(true);
                    policy.setSpecification(
                            ((SyncPolicyTO) policyTO).getSpecification());
                    break;

                case SYNC:
                default:
                    policy = new SyncPolicy();
                    policy.setSpecification(
                            ((SyncPolicyTO) policyTO).getSpecification());
            }

            policy.setId(policyTO.getId());
            policy.setDescription(policyTO.getDescription());
        } else {
            policy = null;
        }

        return policy;
    }
}
