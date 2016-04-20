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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPARealmDAO extends AbstractDAO<Realm> implements RealmDAO {

    @Autowired
    private RoleDAO roleDAO;

    @Override
    public Realm getRoot() {
        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.parent IS NULL", Realm.class);

        Realm result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("Root realm not found", e);
        }

        return result;
    }

    @Override
    public Realm find(final String key) {
        return entityManager().find(JPARealm.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public Realm findByFullPath(final String fullPath) {
        if (SyncopeConstants.ROOT_REALM.equals(fullPath)) {
            return getRoot();
        }

        if (StringUtils.isBlank(fullPath) || !PATH_PATTERN.matcher(fullPath).matches()) {
            throw new MalformedPathException(fullPath);
        }

        Realm root = getRoot();
        if (root == null) {
            return null;
        }

        Realm current = root;
        for (final String pathElement : fullPath.substring(1).split("/")) {
            current = IterableUtils.find(findChildren(current), new Predicate<Realm>() {

                @Override
                public boolean evaluate(final Realm realm) {
                    return pathElement.equals(realm.getName());
                }
            });
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private <T extends Policy> List<Realm> findSamePolicyChildren(final Realm realm, final T policy) {
        List<Realm> result = new ArrayList<>();

        for (Realm child : findChildren(realm)) {
            if ((policy instanceof AccountPolicy
                    && child.getAccountPolicy() == null || policy.equals(child.getAccountPolicy()))
                    || (policy instanceof PasswordPolicy
                    && child.getPasswordPolicy() == null || policy.equals(child.getPasswordPolicy()))) {

                result.add(child);
                result.addAll(findSamePolicyChildren(child, policy));
            }
        }

        return result;
    }

    @Override
    public <T extends Policy> List<Realm> findByPolicy(final T policy) {
        if (PullPolicy.class.isAssignableFrom(policy.getClass())) {
            return Collections.<Realm>emptyList();
        }

        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e."
                + (policy instanceof AccountPolicy ? "accountPolicy" : "passwordPolicy") + "=:policy", Realm.class);
        query.setParameter("policy", policy);

        List<Realm> result = new ArrayList<>();
        for (Realm realm : query.getResultList()) {
            result.add(realm);
            result.addAll(findSamePolicyChildren(realm, policy));
        }
        return result;
    }

    private void findAncestors(final List<Realm> result, final Realm realm) {
        if (realm.getParent() != null && !result.contains(realm.getParent())) {
            result.add(realm.getParent());
            findAncestors(result, realm.getParent());
        }
    }

    @Override
    public List<Realm> findAncestors(final Realm realm) {
        List<Realm> result = new ArrayList<>();
        result.add(realm);
        findAncestors(result, realm);
        return result;
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.parent=:realm", Realm.class);
        query.setParameter("realm", realm);

        return query.getResultList();
    }

    private void findDescendants(final List<Realm> result, final Realm realm) {
        result.add(realm);
        List<Realm> children = findChildren(realm);
        if (children != null) {
            for (Realm child : children) {
                findDescendants(result, child);
            }
        }
    }

    @Override
    public List<Realm> findDescendants(final Realm realm) {
        List<Realm> result = new ArrayList<>();
        findDescendants(result, realm);
        return result;
    }

    @Override
    public List<Realm> findAll() {
        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e ", Realm.class);
        return query.getResultList();
    }

    @Override
    public Realm save(final Realm realm) {
        return entityManager().merge(realm);
    }

    @Override
    public void delete(final Realm realm) {
        for (Realm toBeDeleted : findDescendants(realm)) {
            for (Role role : roleDAO.findByRealm(toBeDeleted)) {
                role.getRealms().remove(toBeDeleted);
            }

            toBeDeleted.setParent(null);

            entityManager().remove(toBeDeleted);
        }
    }

    @Override
    public void delete(final String key) {
        Realm realm = find(key);
        if (realm == null) {
            return;
        }

        delete(realm);
    }

}
