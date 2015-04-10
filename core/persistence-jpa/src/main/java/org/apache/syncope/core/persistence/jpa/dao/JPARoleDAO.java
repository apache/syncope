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
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.springframework.stereotype.Repository;

@Repository
public class JPARoleDAO extends AbstractDAO<Role, Long> implements RoleDAO {

    @Override
    public Role find(final Long key) {
        return entityManager.find(JPARole.class, key);
    }

    @Override
    public Role find(final String name) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e WHERE e.name=:name", Role.class);
        query.setParameter("name", name);

        Role result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("Found more than one match", e);
        }

        return result;
    }

    @Override
    public List<Role> findByRealm(final Realm realm) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e WHERE :realm MEMBER OF e.realms", Role.class);
        query.setParameter("realm", realm);
        return query.getResultList();
    }

    @Override
    public List<Role> findAll() {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e ", Role.class);
        return query.getResultList();
    }

    @Override
    public Role save(final Role role) {
        return entityManager.merge(role);
    }

    @Override
    public void delete(final Role role) {
        entityManager.remove(role);
    }

    @Override
    public void delete(final Long key) {
        Role role = find(key);
        if (role == null) {
            return;
        }

        delete(role);
    }

}
