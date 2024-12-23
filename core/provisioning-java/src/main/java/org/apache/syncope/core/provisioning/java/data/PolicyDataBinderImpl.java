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

import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.InboundPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.policy.PushPolicyTO;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyDataBinderImpl implements PolicyDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(PolicyDataBinder.class);

    protected final ExternalResourceDAO resourceDAO;

    protected final RealmDAO realmDAO;

    protected final AnyTypeDAO anyTypeDAO;

    protected final ImplementationDAO implementationDAO;

    protected final EntityFactory entityFactory;

    public PolicyDataBinderImpl(
            final ExternalResourceDAO resourceDAO,
            final RealmDAO realmDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final EntityFactory entityFactory) {

        this.resourceDAO = resourceDAO;
        this.realmDAO = realmDAO;
        this.anyTypeDAO = anyTypeDAO;
        this.implementationDAO = implementationDAO;
        this.entityFactory = entityFactory;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Policy> T getPolicy(final T policy, final PolicyTO policyTO) {
        T result = policy;

        if (policyTO instanceof PasswordPolicyTO passwordPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(PasswordPolicy.class);
            }

            PasswordPolicy passwordPolicy = PasswordPolicy.class.cast(result);

            passwordPolicy.setAllowNullPassword(passwordPolicyTO.isAllowNullPassword());
            passwordPolicy.setHistoryLength(passwordPolicyTO.getHistoryLength());

            passwordPolicyTO.getRules().forEach(ruleKey -> implementationDAO.findById(ruleKey).ifPresentOrElse(
                    passwordPolicy::add,
                    () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), ruleKey)));
            // remove all implementations not contained in the TO
            passwordPolicy.getRules().
                    removeIf(implementation -> !passwordPolicyTO.getRules().contains(implementation.getKey()));
        } else if (policyTO instanceof AccountPolicyTO accountPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(AccountPolicy.class);
            }

            AccountPolicy accountPolicy = AccountPolicy.class.cast(result);

            accountPolicy.setMaxAuthenticationAttempts(accountPolicyTO.getMaxAuthenticationAttempts());
            accountPolicy.setPropagateSuspension(accountPolicyTO.isPropagateSuspension());

            accountPolicyTO.getRules().forEach(ruleKey -> implementationDAO.findById(ruleKey).ifPresentOrElse(
                    accountPolicy::add,
                    () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), ruleKey)));
            // remove all implementations not contained in the TO
            accountPolicy.getRules().
                    removeIf(implementation -> !accountPolicyTO.getRules().contains(implementation.getKey()));
        } else if (policyTO instanceof PropagationPolicyTO propagationPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(PropagationPolicy.class);
            }

            PropagationPolicy propagationPolicy = PropagationPolicy.class.cast(result);

            propagationPolicy.setFetchAroundProvisioning(propagationPolicyTO.isFetchAroundProvisioning());
            propagationPolicy.setUpdateDelta(propagationPolicyTO.isUpdateDelta());
            propagationPolicy.setBackOffStrategy(propagationPolicyTO.getBackOffStrategy());
            propagationPolicy.setBackOffParams(propagationPolicyTO.getBackOffParams());
            propagationPolicy.setMaxAttempts(propagationPolicyTO.getMaxAttempts());
        } else if (policyTO instanceof InboundPolicyTO inboundPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(InboundPolicy.class);
            }

            InboundPolicy inboundPolicy = InboundPolicy.class.cast(result);

            inboundPolicy.setConflictResolutionAction(inboundPolicyTO.getConflictResolutionAction());

            inboundPolicyTO.getCorrelationRules().forEach((type, impl) -> anyTypeDAO.findById(type).ifPresentOrElse(
                    anyType -> {
                        InboundCorrelationRuleEntity correlationRule = inboundPolicy.
                                getCorrelationRule(anyType.getKey()).orElse(null);
                        if (correlationRule == null) {
                            correlationRule = entityFactory.newEntity(InboundCorrelationRuleEntity.class);
                            correlationRule.setAnyType(anyType);
                            correlationRule.setInboundPolicy(inboundPolicy);
                            inboundPolicy.add(correlationRule);
                        }

                        Implementation rule = implementationDAO.findById(impl).
                                orElseThrow(() -> new NotFoundException("Implementation " + type + ' ' + impl));
                        correlationRule.setImplementation(rule);
                    },
                    () -> LOG.debug("Invalid AnyType {} specified, ignoring...", type)));
            // remove all rules not contained in the TO
            inboundPolicy.getCorrelationRules().removeIf(anyFilter -> !inboundPolicyTO.getCorrelationRules().
                    containsKey(anyFilter.getAnyType().getKey()));
        } else if (policyTO instanceof PushPolicyTO pushPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(PushPolicy.class);
            }

            PushPolicy pushPolicy = PushPolicy.class.cast(result);

            pushPolicy.setConflictResolutionAction(pushPolicyTO.getConflictResolutionAction());

            pushPolicyTO.getCorrelationRules().forEach((type, impl) -> anyTypeDAO.findById(type).ifPresentOrElse(
                    anyType -> {
                        PushCorrelationRuleEntity correlationRule = pushPolicy.
                                getCorrelationRule(anyType.getKey()).orElse(null);
                        if (correlationRule == null) {
                            correlationRule = entityFactory.newEntity(PushCorrelationRuleEntity.class);
                            correlationRule.setAnyType(anyType);
                            correlationRule.setPushPolicy(pushPolicy);
                            pushPolicy.add(correlationRule);
                        }

                        Implementation rule = implementationDAO.findById(impl).
                                orElseThrow(() -> new NotFoundException("Implementation " + type + ' ' + impl));
                        correlationRule.setImplementation(rule);
                    },
                    () -> LOG.debug("Invalid AnyType {} specified, ignoring...", type)));
            // remove all rules not contained in the TO
            pushPolicy.getCorrelationRules().
                    removeIf(anyFilter -> !pushPolicyTO.getCorrelationRules().
                    containsKey(anyFilter.getAnyType().getKey()));
        } else if (policyTO instanceof AuthPolicyTO authPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(AuthPolicy.class);
            }

            AuthPolicy authPolicy = AuthPolicy.class.cast(result);

            authPolicy.setName(authPolicyTO.getKey());
            authPolicy.setConf(authPolicyTO.getConf());
        } else if (policyTO instanceof AccessPolicyTO accessPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(AccessPolicy.class);
            }

            AccessPolicy accessPolicy = AccessPolicy.class.cast(result);

            accessPolicy.setName(accessPolicyTO.getKey());
            accessPolicy.setConf(accessPolicyTO.getConf());
        } else if (policyTO instanceof AttrReleasePolicyTO attrReleasePolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(AttrReleasePolicy.class);
            }

            AttrReleasePolicy attrReleasePolicy = AttrReleasePolicy.class.cast(result);

            attrReleasePolicy.setName(attrReleasePolicyTO.getKey());
            attrReleasePolicy.setOrder(attrReleasePolicyTO.getOrder());
            attrReleasePolicy.setStatus(attrReleasePolicyTO.getStatus());
            attrReleasePolicy.setConf(attrReleasePolicyTO.getConf());
        } else if (policyTO instanceof TicketExpirationPolicyTO ticketExpirationPolicyTO) {
            if (result == null) {
                result = (T) entityFactory.newEntity(TicketExpirationPolicy.class);
            }

            TicketExpirationPolicy ticketExpirationPolicy = TicketExpirationPolicy.class.cast(result);

            ticketExpirationPolicy.setConf(ticketExpirationPolicyTO.getConf());
        }

        if (result != null) {
            result.setName(policyTO.getName());
        }

        return result;
    }

    @Override
    public <T extends Policy> T create(final PolicyTO policyTO) {
        return getPolicy(null, policyTO);
    }

    @Override
    public <T extends Policy> T update(final T policy, final PolicyTO policyTO) {
        return getPolicy(policy, policyTO);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> T getPolicyTO(final Policy policy) {
        T policyTO = null;

        if (policy instanceof PasswordPolicy) {
            PasswordPolicy passwordPolicy = PasswordPolicy.class.cast(policy);
            PasswordPolicyTO passwordPolicyTO = new PasswordPolicyTO();
            policyTO = (T) passwordPolicyTO;

            passwordPolicyTO.setAllowNullPassword(passwordPolicy.isAllowNullPassword());
            passwordPolicyTO.setHistoryLength(passwordPolicy.getHistoryLength());

            passwordPolicyTO.getRules().addAll(
                    passwordPolicy.getRules().stream().map(Implementation::getKey).toList());
        } else if (policy instanceof AccountPolicy) {
            AccountPolicy accountPolicy = AccountPolicy.class.cast(policy);
            AccountPolicyTO accountPolicyTO = new AccountPolicyTO();
            policyTO = (T) accountPolicyTO;

            accountPolicyTO.setMaxAuthenticationAttempts(accountPolicy.getMaxAuthenticationAttempts());
            accountPolicyTO.setPropagateSuspension(accountPolicy.isPropagateSuspension());

            accountPolicyTO.getRules().addAll(
                    accountPolicy.getRules().stream().map(Implementation::getKey).toList());

            accountPolicyTO.getPassthroughResources().addAll(
                    resourceDAO.findByPolicy(accountPolicy).stream().map(ExternalResource::getKey).toList());
        } else if (policy instanceof PropagationPolicy) {
            PropagationPolicy propagationPolicy = PropagationPolicy.class.cast(policy);
            PropagationPolicyTO propagationPolicyTO = new PropagationPolicyTO();
            policyTO = (T) propagationPolicyTO;

            propagationPolicyTO.setFetchAroundProvisioning(propagationPolicy.isFetchAroundProvisioning());
            propagationPolicyTO.setUpdateDelta(propagationPolicy.isUpdateDelta());
            propagationPolicyTO.setBackOffStrategy(propagationPolicy.getBackOffStrategy());
            propagationPolicyTO.setBackOffParams(propagationPolicy.getBackOffParams());
            propagationPolicyTO.setMaxAttempts(propagationPolicy.getMaxAttempts());
        } else if (policy instanceof InboundPolicy inboundPolicy) {
            InboundPolicyTO inboundPolicyTO = new InboundPolicyTO();
            policyTO = (T) inboundPolicyTO;

            inboundPolicyTO.setConflictResolutionAction(inboundPolicy.getConflictResolutionAction());
            inboundPolicy.getCorrelationRules().
                    forEach(rule -> inboundPolicyTO.getCorrelationRules().
                    put(rule.getAnyType().getKey(), rule.getImplementation().getKey()));
        } else if (policy instanceof PushPolicy pushPolicy) {
            PushPolicyTO pushPolicyTO = new PushPolicyTO();
            policyTO = (T) pushPolicyTO;

            pushPolicyTO.setConflictResolutionAction(pushPolicy.getConflictResolutionAction());
            pushPolicy.getCorrelationRules().
                    forEach(rule -> pushPolicyTO.getCorrelationRules().
                    put(rule.getAnyType().getKey(), rule.getImplementation().getKey()));
        } else if (policy instanceof AuthPolicy authPolicy) {
            AuthPolicyTO authPolicyTO = new AuthPolicyTO();
            policyTO = (T) authPolicyTO;

            authPolicyTO.setConf(authPolicy.getConf());
        } else if (policy instanceof AccessPolicy accessPolicy) {
            AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
            policyTO = (T) accessPolicyTO;

            accessPolicyTO.setConf(accessPolicy.getConf());
        } else if (policy instanceof AttrReleasePolicy) {
            AttrReleasePolicy attrReleasePolicy = AttrReleasePolicy.class.cast(policy);
            AttrReleasePolicyTO attrReleasePolicyTO = new AttrReleasePolicyTO();
            policyTO = (T) attrReleasePolicyTO;

            attrReleasePolicyTO.setOrder(attrReleasePolicy.getOrder());
            attrReleasePolicyTO.setStatus(attrReleasePolicy.getStatus());
            attrReleasePolicyTO.setConf(attrReleasePolicy.getConf());
        } else if (policy instanceof TicketExpirationPolicy) {
            TicketExpirationPolicy ticketExpirationPolicy = TicketExpirationPolicy.class.cast(policy);
            TicketExpirationPolicyTO ticketExpirationPolicyTO = new TicketExpirationPolicyTO();
            policyTO = (T) ticketExpirationPolicyTO;

            ticketExpirationPolicyTO.setConf(ticketExpirationPolicy.getConf());
        }

        if (policyTO != null) {
            policyTO.setKey(policy.getKey());
            policyTO.setName(policy.getName());

            if (!(policy instanceof AuthPolicy)
                    && !(policy instanceof AccessPolicy)
                    && !(policy instanceof AttrReleasePolicy)
                    && !(policy instanceof TicketExpirationPolicy)) {

                policyTO.getUsedByResources().addAll(
                        resourceDAO.findByPolicy(policy).stream().map(ExternalResource::getKey).toList());
            }

            policyTO.getUsedByRealms().addAll(realmDAO.findByPolicy(policy).stream().map(Realm::getFullPath).toList());
        }

        return policyTO;
    }
}
