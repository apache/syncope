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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.AccountPolicyMod;
import org.syncope.client.mod.PasswordPolicyMod;
import org.syncope.client.mod.PolicyMod;
import org.syncope.client.mod.SyncPolicyMod;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.types.AccountPolicy;
import org.syncope.types.PasswordPolicy;
import org.syncope.types.SyncPolicy;

@Component
public class PolicyDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            PolicyDataBinder.class);

    /**
     * Get policy TO from policy bean.
     * @param policy bean.
     * @return policy TO.
     */
    public PolicyTO getPolicyTO(Policy policy) {

        final PolicyTO policyTO;

        if (policy != null) {
            switch (policy.getType()) {
                case GLOBAL_PASSWORD:
                case PASSWORD:
                    policyTO = new PasswordPolicyTO();
                    ((PasswordPolicyTO) policyTO).setSpecification(
                            (PasswordPolicy) policy.getSpecification());
                    break;
                case GLOBAL_ACCOUNT:
                case ACCOUNT:
                    policyTO = new AccountPolicyTO();
                    ((AccountPolicyTO) policyTO).setSpecification(
                            (AccountPolicy) policy.getSpecification());
                    break;
                default:
                    policyTO = new SyncPolicyTO();
                    ((SyncPolicyTO) policyTO).setSpecification(
                            (SyncPolicy) policy.getSpecification());

            }

            policyTO.setId(policy.getId());
            policyTO.setType(policy.getType());
            policyTO.setDescription(policy.getDescription());
        } else {
            policyTO = null;
        }

        return policyTO;
    }

    /**
     * Get policy bean from policy TO.
     * @param policy TO.
     * @return policy bean.
     */
    public Policy getPolicy(PolicyTO policyTO) {

        final Policy policy;

        if (policyTO != null) {
            policy = new Policy();
            policy.setId(policyTO.getId());
            policy.setType(policyTO.getType());
            policy.setDescription(policyTO.getDescription());

            switch (policy.getType()) {
                case GLOBAL_PASSWORD:
                case PASSWORD:
                    policy.setSpecification(
                            ((PasswordPolicyTO) policyTO).getSpecification());
                    break;
                case GLOBAL_ACCOUNT:
                case ACCOUNT:
                    policy.setSpecification(
                            ((AccountPolicyTO) policyTO).getSpecification());
                    break;
                default:
                    policy.setSpecification(
                            ((SyncPolicyTO) policyTO).getSpecification());

            }
        } else {
            policy = null;
        }

        return policy;
    }

    /**
     * Get policy bean from policy mod.
     * @param policy mod.
     * @return policy bean.
     */
    public Policy getPolicy(PolicyMod policyMod) {

        final Policy policy;

        if (policyMod != null) {
            policy = new Policy();
            policy.setId(policyMod.getId());
            policy.setType(policyMod.getType());
            policy.setDescription(policyMod.getDescription());

            switch (policy.getType()) {
                case GLOBAL_PASSWORD:
                case PASSWORD:
                    policy.setSpecification(
                            ((PasswordPolicyMod) policyMod).getSpecification());
                    break;
                case GLOBAL_ACCOUNT:
                case ACCOUNT:
                    policy.setSpecification(
                            ((AccountPolicyMod) policyMod).getSpecification());
                    break;
                default:
                    policy.setSpecification(
                            ((SyncPolicyMod) policyMod).getSpecification());
            }
        } else {
            policy = null;
        }

        return policy;
    }
}
