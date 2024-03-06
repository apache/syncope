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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class RoleRepoExtImpl implements RoleRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final AnyMatchDAO anyMatchDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final DelegationDAO delegationDAO;

    protected final SearchCondVisitor searchCondVisitor;

    protected final EntityManager entityManager;

    public RoleRepoExtImpl(
            final ApplicationEventPublisher publisher,
            final AnyMatchDAO anyMatchDAO,
            final AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager) {

        this.publisher = publisher;
        this.anyMatchDAO = anyMatchDAO;
        this.anySearchDAO = anySearchDAO;
        this.delegationDAO = delegationDAO;
        this.searchCondVisitor = searchCondVisitor;
        this.entityManager = entityManager;
    }

    @Override
    public Role save(final Role role) {
        ((JPARole) role).list2json();
        return entityManager.merge(role);
    }

    @Override
    public Role saveAndRefreshDynMemberships(final Role role) {
        Role merged = save(role);

        // refresh dynamic memberships
        clearDynMembers(merged);
        if (merged.getDynMembershipCond() != null) {
            List<User> matching = anySearchDAO.search(
                    SearchCondConverter.convert(searchCondVisitor, merged.getDynMembershipCond()),
                    AnyTypeKind.USER);

            matching.forEach(user -> {
                Query insert = entityManager.createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, merged.getKey());
                insert.executeUpdate();

                publisher.publishEvent(
                        new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));
            });
        }

        return merged;
    }

    @Override
    public void delete(final Role role) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE :role MEMBER OF e.roles", User.class);
        query.setParameter("role", role);

        query.getResultList().forEach(user -> {
            user.getRoles().remove(role);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));
        });

        clearDynMembers(role);

        delegationDAO.findByRoles(role).forEach(delegation -> delegation.getRoles().remove(role));

        entityManager.remove(role);
    }

    @Override
    public List<String> findDynMembers(final Role role) {
        if (role.getDynMembershipCond() == null) {
            return List.of();
        }

        Query query = entityManager.createNativeQuery("SELECT any_id FROM " + DYNMEMB_TABLE + " WHERE role_id=?");
        query.setParameter(1, role.getKey());

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();
        return result.stream().
                map(Object::toString).
                toList();
    }

    @Override
    public void clearDynMembers(final Role role) {
        Query delete = entityManager.createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE role_id=?");
        delete.setParameter(1, role.getKey());
        delete.executeUpdate();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final User user) {
        entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e "
                + "WHERE e.dynMembershipCond IS NOT NULL", Role.class).getResultStream().forEach(role -> {
                    boolean matches = anyMatchDAO.matches(
                            user,
                            SearchCondConverter.convert(searchCondVisitor, role.getDynMembershipCond()));

                    Query query = entityManager.createNativeQuery(
                            "SELECT COUNT(role_id) FROM " + DYNMEMB_TABLE + " WHERE any_id=? AND role_id=?");
                    query.setParameter(1, user.getKey());
                    query.setParameter(2, role.getKey());
                    boolean existing = ((Number) query.getSingleResult()).longValue() > 0;

                    if (matches && !existing) {
                        Query insert = entityManager.createNativeQuery(
                                "INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                        insert.setParameter(1, user.getKey());
                        insert.setParameter(2, role.getKey());
                        insert.executeUpdate();
                    } else if (!matches && existing) {
                        Query delete = entityManager.createNativeQuery(
                                "DELETE FROM " + DYNMEMB_TABLE + " WHERE role_id=? AND any_id=?");
                        delete.setParameter(1, role.getKey());
                        delete.setParameter(2, user.getKey());
                        delete.executeUpdate();
                    }
                });
    }

    @Override
    public void removeDynMemberships(final String key) {
        Query delete = entityManager.createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, key);
        delete.executeUpdate();
    }
}
