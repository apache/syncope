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
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class JPARoleDAO extends AbstractDAO<Role> implements RoleDAO {

    public static final String DYNMEMB_TABLE = "DynRoleMembers";

    protected final AnyMatchDAO anyMatchDAO;

    protected final ApplicationEventPublisher publisher;

    protected final AnySearchDAO anySearchDAO;

    protected final DelegationDAO delegationDAO;

    protected final SearchCondVisitor searchCondVisitor;

    public JPARoleDAO(
            final AnyMatchDAO anyMatchDAO,
            final ApplicationEventPublisher publisher,
            final AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor) {

        this.anyMatchDAO = anyMatchDAO;
        this.publisher = publisher;
        this.anySearchDAO = anySearchDAO;
        this.delegationDAO = delegationDAO;
        this.searchCondVisitor = searchCondVisitor;
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
    public List<Role> findByPrivilege(final Privilege privilege) {
        TypedQuery<Role> query = entityManager().createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e WHERE :privilege MEMBER OF e.privileges",
                Role.class);
        query.setParameter("privilege", privilege);
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
        return entityManager().merge(role);
    }

    @Override
    public Role saveAndRefreshDynMemberships(final Role role) {
        Role merged = save(role);

        // refresh dynamic memberships
        clearDynMembers(merged);
        if (merged.getDynMembership() != null) {
            List<User> matching = anySearchDAO.search(
                    SearchCondConverter.convert(searchCondVisitor, merged.getDynMembership().getFIQLCond()),
                    AnyTypeKind.USER);

            matching.forEach((user) -> {
                Query insert = entityManager().createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, merged.getKey());
                insert.executeUpdate();

                publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user, AuthContextUtils.getDomain()));
            });
        }

        return merged;
    }

    @Override
    public void delete(final Role role) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE :role MEMBER OF e.roles", User.class);
        query.setParameter("role", role);

        query.getResultList().forEach(user -> {
            user.getRoles().remove(role);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user, AuthContextUtils.getDomain()));
        });

        clearDynMembers(role);

        delegationDAO.findByRole(role).forEach(delegation -> delegation.getRoles().remove(role));

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
    @SuppressWarnings("unchecked")
    public List<String> findDynMembers(final Role role) {
        if (role.getDynMembership() == null) {
            return List.of();
        }

        Query query = entityManager().createNativeQuery("SELECT any_id FROM " + DYNMEMB_TABLE + " WHERE role_id=?");
        query.setParameter(1, role.getKey());

        List<String> result = new ArrayList<>();
        query.getResultList().stream().map(key -> key instanceof Object[]
                ? (String) ((Object[]) key)[0]
                : ((String) key)).
                forEach(user -> result.add((String) user));
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
        Query query = entityManager().createNativeQuery(
                "SELECT role_id FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, user.getKey());

        findAll().stream().filter(role -> role.getDynMembership() != null).forEach(role -> {
            boolean matches = anyMatchDAO.matches(
                    user, SearchCondConverter.convert(searchCondVisitor, role.getDynMembership().getFIQLCond()));

            Query find = entityManager().createNativeQuery(
                    "SELECT any_id FROM " + DYNMEMB_TABLE + " WHERE role_id=?");
            find.setParameter(1, role.getKey());
            boolean existing = !find.getResultList().isEmpty();

            if (matches && !existing) {
                Query insert = entityManager().createNativeQuery(
                        "INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, role.getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager().createNativeQuery(
                        "DELETE FROM " + DYNMEMB_TABLE + " WHERE role_id=? AND any_id=?");
                delete.setParameter(1, role.getKey());
                delete.setParameter(2, user.getKey());
                delete.executeUpdate();
            }
        });
    }

    @Override
    public void removeDynMemberships(final String key) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, key);
        delete.executeUpdate();
    }
}
