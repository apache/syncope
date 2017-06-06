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
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPARoleDAO extends AbstractDAO<Role> implements RoleDAO {

    public static final String DYNMEMB_TABLE = "DynRoleMembers";

    @Autowired
    private ApplicationEventPublisher publisher;

    private AnySearchDAO searchDAO;

    private AnySearchDAO searchDAO() {
        synchronized (this) {
            if (searchDAO == null) {
                searchDAO = ApplicationContextProvider.getApplicationContext().getBean(AnySearchDAO.class);
            }
        }
        return searchDAO;
    }

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
        Role merged = entityManager().merge(role);

        // refresh dynamic memberships
        if (merged.getDynMembership() != null) {
            List<User> matching = searchDAO().search(
                    SearchCondConverter.convert(merged.getDynMembership().getFIQLCond()), AnyTypeKind.USER);

            clearDynMembers(merged);

            for (User user : matching) {
                Query insert = entityManager().createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, merged.getKey());
                insert.executeUpdate();

                publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user, AuthContextUtils.getDomain()));
            }
        }

        return merged;
    }

    @Override
    public void delete(final Role role) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE :role MEMBER OF e.roles", User.class);
        query.setParameter("role", role);

        for (User user : query.getResultList()) {
            user.getRoles().remove(role);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user, AuthContextUtils.getDomain()));
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

    @Override
    public List<String> findDynMembers(final Role role) {
        if (role.getDynMembership() == null) {
            return Collections.emptyList();
        }

        Query query = entityManager().createNativeQuery("SELECT any_id FROM " + DYNMEMB_TABLE + " WHERE role_id=?");
        query.setParameter(1, role.getKey());

        List<String> result = new ArrayList<>();
        for (Object key : query.getResultList()) {
            String actualKey = key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key);

            result.add(actualKey);
        }
        return result;
    }

    @Override
    public void clearDynMembers(final Role role) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE role_id=?");
        delete.setParameter(1, role.getKey());
        delete.executeUpdate();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final User user) {
        for (Role role : findAll()) {
            if (role.getDynMembership() != null) {
                Query delete = entityManager().createNativeQuery(
                        "DELETE FROM " + DYNMEMB_TABLE + " WHERE role_id=? AND any_id=?");
                delete.setParameter(1, role.getKey());
                delete.setParameter(2, user.getKey());
                delete.executeUpdate();

                if (searchDAO().matches(user, SearchCondConverter.convert(role.getDynMembership().getFIQLCond()))) {
                    Query insert = entityManager().createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                    insert.setParameter(1, user.getKey());
                    insert.setParameter(2, role.getKey());
                    insert.executeUpdate();
                }
            }
        }
    }

    @Override
    public void removeDynMemberships(final User user) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, user.getKey());
        delete.executeUpdate();
    }

}
