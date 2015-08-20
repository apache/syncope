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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.policy.AbstractPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPolicyDAO extends AbstractDAO<Policy, Long> implements PolicyDAO {

    @Autowired
    private RealmDAO realmDAO;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Policy> T find(final Long key) {
        final Query query = entityManager().createQuery(
                "SELECT e FROM " + AbstractPolicy.class.getSimpleName() + " e WHERE e.id=:id");
        query.setParameter("id", key);

        List<T> result = query.getResultList();
        return result.isEmpty()
                ? null
                : result.iterator().next();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Policy> List<T> find(final PolicyType type) {
        final Query query = entityManager().createQuery(
                "SELECT e FROM " + AbstractPolicy.class.getSimpleName() + " e WHERE e.type=:type");
        query.setParameter("type", type);

        return (List<T>) query.getResultList();
    }

    @Override
    public List<AccountPolicy> findByResource(final ExternalResource resource) {
        TypedQuery<AccountPolicy> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAccountPolicy.class.getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources", AccountPolicy.class);
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public List<Policy> findAll() {
        TypedQuery<Policy> query = entityManager().createQuery(
                "SELECT e FROM " + AbstractPolicy.class.getSimpleName() + " e", Policy.class);
        return query.getResultList();
    }

    @Override
    public <T extends Policy> T save(final T policy) {
        return entityManager().merge(policy);
    }

    @Override
    public <T extends Policy> void delete(final T policy) {
        for (Realm realm : realmDAO.findByPolicy(policy)) {
            if (policy instanceof AccountPolicy) {
                realm.setAccountPolicy(null);
            } else if (policy instanceof PasswordPolicy) {
                realm.setPasswordPolicy(null);
            }
        }

        entityManager().remove(policy);
    }
}
