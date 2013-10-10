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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.springframework.stereotype.Repository;

@Repository
public class PolicyDAOImpl extends AbstractDAOImpl implements PolicyDAO {

    @Override
    public Policy find(final Long id) {
        return entityManager.find(Policy.class, id);
    }

    @Override
    public List<? extends Policy> find(final PolicyType type) {
        final TypedQuery<Policy> query = entityManager.createQuery("SELECT e FROM Policy e WHERE e.type=:type",
                Policy.class);
        query.setParameter("type", type);

        return query.getResultList();
    }

    @Override
    public PasswordPolicy getGlobalPasswordPolicy() {
        List<? extends Policy> policies = find(PolicyType.GLOBAL_PASSWORD);
        return policies == null || policies.isEmpty()
                ? null
                : (PasswordPolicy) policies.get(0);
    }

    @Override
    public AccountPolicy getGlobalAccountPolicy() {
        List<? extends Policy> policies = find(PolicyType.GLOBAL_ACCOUNT);
        return policies == null || policies.isEmpty()
                ? null
                : (AccountPolicy) policies.get(0);
    }

    @Override
    public SyncPolicy getGlobalSyncPolicy() {
        List<? extends Policy> policies = find(PolicyType.GLOBAL_SYNC);
        return policies == null || policies.isEmpty()
                ? null
                : (SyncPolicy) policies.get(0);
    }

    @Override
    public List<Policy> findAll() {
        TypedQuery<Policy> query = entityManager.createQuery("SELECT e FROM Policy e", Policy.class);
        return query.getResultList();
    }

    @Override
    public <T extends Policy> T save(final T policy) {
        switch (policy.getType()) {
            case GLOBAL_PASSWORD:
                // just one GLOBAL_PASSWORD policy
                final PasswordPolicy passwordPolicy = getGlobalPasswordPolicy();

                if (passwordPolicy != null && !passwordPolicy.getId().equals(policy.getId())) {
                    throw new InvalidEntityException(PasswordPolicy.class, EntityViolationType.InvalidPasswordPolicy,
                            "Global Password policy already exists");
                }
                break;

            case GLOBAL_ACCOUNT:
                // just one GLOBAL_ACCOUNT policy
                final AccountPolicy accountPolicy = getGlobalAccountPolicy();

                if (accountPolicy != null && !accountPolicy.getId().equals(policy.getId())) {
                    throw new InvalidEntityException(PasswordPolicy.class, EntityViolationType.InvalidAccountPolicy,
                            "Global Account policy already exists");
                }
                break;

            case GLOBAL_SYNC:
                // just one GLOBAL_SYNC policy
                final SyncPolicy syncPolicy = getGlobalSyncPolicy();

                if (syncPolicy != null && !syncPolicy.getId().equals(policy.getId())) {
                    throw new InvalidEntityException(PasswordPolicy.class, EntityViolationType.InvalidSyncPolicy,
                            "Global Synchronization policy already exists");
                }
                break;

            case PASSWORD:
            case ACCOUNT:
            case SYNC:
            default:
        }

        return entityManager.merge(policy);
    }

    @Override
    public <T extends Policy> void delete(final T policy) {
        entityManager.remove(policy);
    }
}
