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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.apache.syncope.common.lib.policy.AbstractAccountRuleConf;
import org.apache.syncope.common.lib.policy.AbstractPasswordRuleConf;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;

@Component
public class PolicyDataBinderImpl implements PolicyDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyDataBinder.class);

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private EntityFactory entityFactory;

    @SuppressWarnings("unchecked")
    private <T extends Policy> T getPolicy(final T policy, final AbstractPolicyTO policyTO) {
        T result = policy;

        if (policyTO instanceof PasswordPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(PasswordPolicy.class);
            }

            PasswordPolicy passwordPolicy = PasswordPolicy.class.cast(result);
            PasswordPolicyTO passwordPolicyTO = PasswordPolicyTO.class.cast(policyTO);

            passwordPolicy.setAllowNullPassword(passwordPolicyTO.isAllowNullPassword());
            passwordPolicy.setHistoryLength(passwordPolicyTO.getHistoryLength());

            passwordPolicy.removeAllRuleConfs();
            for (PasswordRuleConf conf : passwordPolicyTO.getRuleConfs()) {
                passwordPolicy.add(conf);
            }
        } else if (policyTO instanceof AccountPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(AccountPolicy.class);
            }

            AccountPolicy accountPolicy = AccountPolicy.class.cast(result);
            AccountPolicyTO accountPolicyTO = AccountPolicyTO.class.cast(policyTO);

            accountPolicy.setMaxAuthenticationAttempts(accountPolicyTO.getMaxAuthenticationAttempts());
            accountPolicy.setPropagateSuspension(accountPolicyTO.isPropagateSuspension());

            accountPolicy.removeAllRuleConfs();
            for (AccountRuleConf conf : accountPolicyTO.getRuleConfs()) {
                accountPolicy.add(conf);
            }

            accountPolicy.getResources().clear();
            for (String resourceName : accountPolicyTO.getPassthroughResources()) {
                ExternalResource resource = resourceDAO.find(resourceName);
                if (resource == null) {
                    LOG.debug("Ignoring invalid resource {} ", resourceName);
                } else {
                    accountPolicy.add(resource);
                }
            }
        } else if (policyTO instanceof PullPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(PullPolicy.class);
            }

            ((PullPolicy) result).setSpecification(((PullPolicyTO) policyTO).getSpecification());
        }

        if (result != null) {
            result.setDescription(policyTO.getDescription());
        }

        return result;
    }

    @Override
    public <T extends Policy> T create(final AbstractPolicyTO policyTO) {
        return getPolicy(null, policyTO);
    }

    @Override
    public <T extends Policy> T update(final T policy, final AbstractPolicyTO policyTO) {
        return getPolicy(policy, policyTO);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractPolicyTO> T getPolicyTO(final Policy policy) {
        T policyTO = null;

        if (policy instanceof PasswordPolicy) {
            PasswordPolicy passwordPolicy = PasswordPolicy.class.cast(policy);
            PasswordPolicyTO passwordPolicyTO = new PasswordPolicyTO();
            policyTO = (T) passwordPolicyTO;

            passwordPolicyTO.setAllowNullPassword(passwordPolicy.isAllowNullPassword());
            passwordPolicyTO.setHistoryLength(passwordPolicy.getHistoryLength());

            for (PasswordRuleConf ruleConf : passwordPolicy.getRuleConfs()) {
                passwordPolicyTO.getRuleConfs().add((AbstractPasswordRuleConf) ruleConf);
            }
        } else if (policy instanceof AccountPolicy) {
            AccountPolicy accountPolicy = AccountPolicy.class.cast(policy);
            AccountPolicyTO accountPolicyTO = new AccountPolicyTO();
            policyTO = (T) accountPolicyTO;

            accountPolicyTO.setMaxAuthenticationAttempts(accountPolicy.getMaxAuthenticationAttempts());
            accountPolicyTO.setPropagateSuspension(accountPolicy.isPropagateSuspension());

            for (AccountRuleConf ruleConf : accountPolicy.getRuleConfs()) {
                accountPolicyTO.getRuleConfs().add((AbstractAccountRuleConf) ruleConf);
            }

            accountPolicyTO.getPassthroughResources().addAll(accountPolicy.getResourceKeys());
        } else if (policy instanceof PullPolicy) {
            policyTO = (T) new PullPolicyTO();
            ((PullPolicyTO) policyTO).setSpecification(((PullPolicy) policy).getSpecification());
        }

        if (policyTO != null) {
            policyTO.setKey(policy.getKey());
            policyTO.setDescription(policy.getDescription());

            for (ExternalResource resource : resourceDAO.findByPolicy(policy)) {
                policyTO.getUsedByResources().add(resource.getKey());
            }
            for (Realm realm : realmDAO.findByPolicy(policy)) {
                policyTO.getUsedByRealms().add(realm.getFullPath());
            }
        }

        return policyTO;
    }
}
