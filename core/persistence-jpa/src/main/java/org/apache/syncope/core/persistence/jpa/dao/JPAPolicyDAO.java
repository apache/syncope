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
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.policy.AbstractPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccessPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAttrReleasePolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPropagationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPullPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushPolicy;

public class JPAPolicyDAO extends AbstractDAO<Policy> implements PolicyDAO {

    protected static <T extends Policy> Class<? extends AbstractPolicy> getEntityReference(final Class<T> reference) {
        return AccountPolicy.class.isAssignableFrom(reference)
                ? JPAAccountPolicy.class
                : PasswordPolicy.class.isAssignableFrom(reference)
                ? JPAPasswordPolicy.class
                : PropagationPolicy.class.isAssignableFrom(reference)
                ? JPAPropagationPolicy.class
                : PullPolicy.class.isAssignableFrom(reference)
                ? JPAPullPolicy.class
                : PushPolicy.class.isAssignableFrom(reference)
                ? JPAPushPolicy.class
                : AuthPolicy.class.isAssignableFrom(reference)
                ? JPAAuthPolicy.class
                : AccessPolicy.class.isAssignableFrom(reference)
                ? JPAAccessPolicy.class
                : AttrReleasePolicy.class.isAssignableFrom(reference)
                ? JPAAttrReleasePolicy.class
                : null;
    }

    protected final RealmDAO realmDAO;

    protected final ExternalResourceDAO resourceDAO;

    public JPAPolicyDAO(final RealmDAO realmDAO, final ExternalResourceDAO resourceDAO) {
        this.realmDAO = realmDAO;
        this.resourceDAO = resourceDAO;
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
    public List<AccountPolicy> findByAccountRule(final Implementation accountRule) {
        TypedQuery<AccountPolicy> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAccountPolicy.class.getSimpleName() + " e "
                + "WHERE :accountRule MEMBER OF e.rules", AccountPolicy.class);
        query.setParameter("accountRule", accountRule);

        return query.getResultList();
    }

    @Override
    public List<PasswordPolicy> findByPasswordRule(final Implementation passwordRule) {
        TypedQuery<PasswordPolicy> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPasswordPolicy.class.getSimpleName() + " e "
                + "WHERE :passwordRule MEMBER OF e.rules", PasswordPolicy.class);
        query.setParameter("passwordRule", passwordRule);

        return query.getResultList();
    }

    @Override
    public List<PullPolicy> findByPullCorrelationRule(final Implementation correlationRule) {
        TypedQuery<PullPolicy> query = entityManager().createQuery(
                "SELECT DISTINCT e.pullPolicy FROM " + JPAPullCorrelationRuleEntity.class.getSimpleName() + " e "
                + "WHERE e.implementation=:correlationRule", PullPolicy.class);
        query.setParameter("correlationRule", correlationRule);

        return query.getResultList();
    }

    @Override
    public List<PushPolicy> findByPushCorrelationRule(final Implementation correlationRule) {
        TypedQuery<PushPolicy> query = entityManager().createQuery(
                "SELECT DISTINCT e.pushPolicy FROM " + JPAPushCorrelationRuleEntity.class.getSimpleName() + " e "
                + "WHERE e.implementation=:correlationRule", PushPolicy.class);
        query.setParameter("correlationRule", correlationRule);

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
        realmDAO.findByPolicy(policy).forEach(realm -> {
            if (policy instanceof AccountPolicy) {
                realm.setAccountPolicy(null);
            } else if (policy instanceof PasswordPolicy) {
                realm.setPasswordPolicy(null);
            } else if (policy instanceof AuthPolicy) {
                realm.setAuthPolicy(null);
            } else if (policy instanceof AccessPolicy) {
                realm.setAccessPolicy(null);
            } else if (policy instanceof AttrReleasePolicy) {
                realm.setAttrReleasePolicy(null);
            }
        });

        if (!(policy instanceof AuthPolicy)
                && !(policy instanceof AttrReleasePolicy)
                && !(policy instanceof AccessPolicy)) {

            resourceDAO.findByPolicy(policy).forEach(resource -> {
                if (policy instanceof AccountPolicy) {
                    resource.setAccountPolicy(null);
                } else if (policy instanceof PasswordPolicy) {
                    resource.setPasswordPolicy(null);
                } else if (policy instanceof PropagationPolicy) {
                    resource.setPropagationPolicy(null);
                } else if (policy instanceof PullPolicy) {
                    resource.setPullPolicy(null);
                } else if (policy instanceof PushPolicy) {
                    resource.setPushPolicy(null);
                }
            });
        }

        entityManager().remove(policy);
    }
}
