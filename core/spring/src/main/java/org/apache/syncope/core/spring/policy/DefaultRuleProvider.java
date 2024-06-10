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
package org.apache.syncope.core.spring.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class DefaultRuleProvider implements RuleProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(RuleProvider.class);

    protected final RealmSearchDAO realmSearchDAO;

    protected final Map<String, AccountRule> perContextAccountRules = new ConcurrentHashMap<>();

    protected final Map<String, PasswordRule> perContextPasswordRules = new ConcurrentHashMap<>();

    public DefaultRuleProvider(final RealmSearchDAO realmSearchDAO) {
        this.realmSearchDAO = realmSearchDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AccountPolicy> getAccountPolicies(final Realm realm, final Collection<ExternalResource> resources) {
        List<AccountPolicy> policies = new ArrayList<>();

        // add resource policies
        resources.forEach(resource -> Optional.ofNullable(resource.getAccountPolicy()).
                filter(p -> !policies.contains(p)).
                ifPresent(policies::add));

        // add realm policies
        if (realm != null) {
            realmSearchDAO.findAncestors(realm).
                    forEach(a -> realmSearchDAO.findByFullPath(a.getFullPath()).
                    flatMap(r -> Optional.ofNullable(r.getAccountPolicy())).
                    filter(p -> !policies.contains(p)).
                    ifPresent(policies::add));
        }

        return policies;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AccountRule> getAccountRules(final AccountPolicy policy) {
        List<AccountRule> result = new ArrayList<>();

        for (Implementation impl : policy.getRules()) {
            try {
                ImplementationManager.buildAccountRule(
                        impl,
                        () -> perContextAccountRules.get(impl.getKey()),
                        instance -> perContextAccountRules.put(impl.getKey(), instance)).
                        ifPresent(result::add);
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<PasswordPolicy> getPasswordPolicies(final Realm realm, final Collection<ExternalResource> resources) {
        List<PasswordPolicy> policies = new ArrayList<>();

        // add resource policies
        resources.forEach(resource -> Optional.ofNullable(resource.getPasswordPolicy()).
                filter(p -> !policies.contains(p)).
                ifPresent(policies::add));

        // add realm policies
        if (realm != null) {
            realmSearchDAO.findAncestors(realm).
                    forEach(a -> realmSearchDAO.findByFullPath(a.getFullPath()).
                    flatMap(r -> Optional.ofNullable(r.getPasswordPolicy())).
                    filter(p -> !policies.contains(p)).
                    ifPresent(policies::add));
        }

        return policies;
    }

    @Transactional(readOnly = true)
    @Override
    public List<PasswordRule> getPasswordRules(final PasswordPolicy policy) {
        List<PasswordRule> result = new ArrayList<>();

        for (Implementation impl : policy.getRules()) {
            try {
                ImplementationManager.buildPasswordRule(
                        impl,
                        () -> perContextPasswordRules.get(impl.getKey()),
                        instance -> perContextPasswordRules.put(impl.getKey(), instance)).
                        ifPresent(result::add);
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        }

        return result;
    }
}
