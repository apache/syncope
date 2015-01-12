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
package org.apache.syncope.server.persistence.jpa.dao;

import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.server.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.server.persistence.api.dao.PolicyDAO;
import org.apache.syncope.server.persistence.api.entity.AccountPolicy;
import org.apache.syncope.server.persistence.api.entity.ExternalResource;
import org.apache.syncope.server.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.server.persistence.api.entity.Policy;
import org.apache.syncope.server.persistence.api.entity.SyncPolicy;
import org.apache.syncope.server.persistence.jpa.entity.JPAPolicy;
import org.apache.syncope.server.persistence.jpa.entity.JPAAccountPolicy;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPolicyDAO extends AbstractDAO<Policy, Long> implements PolicyDAO {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Policy> T find(final Long key) {
        final Query query = entityManager.createQuery(
                "SELECT e FROM " + JPAPolicy.class.getSimpleName() + " e WHERE e.id=:id");
        query.setParameter("id", key);

        List<T> result = query.getResultList();
        return result.isEmpty()
                ? null
                : result.iterator().next();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Policy> List<T> find(final PolicyType type) {
        final Query query = entityManager.createQuery(
                "SELECT e FROM " + JPAPolicy.class.getSimpleName() + " e WHERE e.type=:type");
        query.setParameter("type", type);

        return (List<T>) query.getResultList();
    }

    @Override
    public List<AccountPolicy> findByResource(final ExternalResource resource) {
        TypedQuery<AccountPolicy> query = entityManager.createQuery(
                "SELECT e FROM " + JPAAccountPolicy.class.getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources", AccountPolicy.class);
        query.setParameter("resource", resource);

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
        TypedQuery<Policy> query = entityManager.createQuery(
                "SELECT e FROM " + JPAPolicy.class.getSimpleName() + " e", Policy.class);
        return query.getResultList();
    }

    @Override
    public <T extends Policy> T save(final T policy) {
        switch (policy.getType()) {
            case GLOBAL_PASSWORD:
                // just one GLOBAL_PASSWORD policy
                final PasswordPolicy passwordPolicy = getGlobalPasswordPolicy();

                if (passwordPolicy != null && !passwordPolicy.getKey().equals(policy.getKey())) {
                    throw new InvalidEntityException(PasswordPolicy.class, EntityViolationType.InvalidPasswordPolicy,
                            "Global Password policy already exists");
                }
                break;

            case GLOBAL_ACCOUNT:
                // just one GLOBAL_ACCOUNT policy
                final AccountPolicy accountPolicy = getGlobalAccountPolicy();

                if (accountPolicy != null && !accountPolicy.getKey().equals(policy.getKey())) {
                    throw new InvalidEntityException(PasswordPolicy.class, EntityViolationType.InvalidAccountPolicy,
                            "Global Account policy already exists");
                }
                break;

            case GLOBAL_SYNC:
                // just one GLOBAL_SYNC policy
                final SyncPolicy syncPolicy = getGlobalSyncPolicy();

                if (syncPolicy != null && !syncPolicy.getKey().equals(policy.getKey())) {
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
