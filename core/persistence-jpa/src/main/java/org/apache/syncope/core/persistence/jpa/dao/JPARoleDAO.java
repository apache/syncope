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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPARoleDAO extends AbstractDAO<Role> implements RoleDAO {

    @Autowired
    private AnySearchDAO searchDAO;

    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPARole.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public Role find(final String key) {
        return entityManager().find(JPARole.class, key);
    }

    @Override
    public List<Role> findByRealm(final Realm realm) {
        TypedQuery<Role> query = entityManager().createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e WHERE :realm MEMBER OF e.realms", Role.class);
        query.setParameter("realm", realm);
        return query.getResultList();
    }

    @Override
    public List<Role> findAll() {
        TypedQuery<Role> query = entityManager().createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e ", Role.class);
        return query.getResultList();
    }

    @Override
    public Role save(final Role role) {
        // refresh dynaminc memberships
        if (role.getDynMembership() != null) {
            List<User> matchingUsers = searchDAO.search(
                    SearchCondConverter.convert(role.getDynMembership().getFIQLCond()), AnyTypeKind.USER);

            role.getDynMembership().getMembers().clear();
            for (User user : matchingUsers) {
                role.getDynMembership().add(user);
            }
        }

        return entityManager().merge(role);
    }

    @Override
    public void delete(final Role role) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE :role MEMBER OF e.roles", User.class);
        query.setParameter("role", role);

        for (User user : query.getResultList()) {
            user.getRoles().remove(role);
        }

        entityManager().remove(role);
    }

    @Override
    public void delete(final String key) {
        Role role = find(key);
        if (role == null) {
            return;
        }

        delete(role);
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final User user) {
        for (Role role : findAll()) {
            if (role.getDynMembership() != null) {
                if (searchDAO.matches(user, SearchCondConverter.convert(role.getDynMembership().getFIQLCond()))) {
                    role.getDynMembership().add(user);
                } else {
                    role.getDynMembership().getMembers().remove(user);
                }
            }
        }
    }

}
