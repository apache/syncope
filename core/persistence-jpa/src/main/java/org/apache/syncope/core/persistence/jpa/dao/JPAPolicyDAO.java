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
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.AbstractPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPullPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPolicyDAO extends AbstractDAO<Policy> implements PolicyDAO {

    @Autowired
    private RealmDAO realmDAO;

    private <T extends Policy> Class<? extends AbstractPolicy> getEntityReference(final Class<T> reference) {
        return AccountPolicy.class.isAssignableFrom(reference)
                ? JPAAccountPolicy.class
                : PasswordPolicy.class.isAssignableFrom(reference)
                ? JPAPasswordPolicy.class
                : PullPolicy.class.isAssignableFrom(reference)
                ? JPAPullPolicy.class
                : PushPolicy.class.isAssignableFrom(reference)
                ? JPAPushPolicy.class
                : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Policy> T find(final String key) {
        return (T) entityManager().find(AbstractPolicy.class, key);
    }

    @Override
    public <T extends Policy> List<T> find(final Class<T> reference) {
        TypedQuery<T> query = entityManager().createQuery(
                "SELECT e FROM " + getEntityReference(reference).getSimpleName() + " e", reference);

        return query.getResultList();
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
