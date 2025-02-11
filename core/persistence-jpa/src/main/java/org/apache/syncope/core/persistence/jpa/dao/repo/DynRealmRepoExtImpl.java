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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.entity.JPADynRealm;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class DynRealmRepoExtImpl implements DynRealmRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO searchDAO;

    protected final AnyMatchDAO anyMatchDAO;

    protected final SearchCondVisitor searchCondVisitor;

    protected final EntityManager entityManager;

    public DynRealmRepoExtImpl(
            final ApplicationEventPublisher publisher,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final AnyMatchDAO anyMatchDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager) {

        this.publisher = publisher;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.searchDAO = searchDAO;
        this.anyMatchDAO = anyMatchDAO;
        this.searchCondVisitor = searchCondVisitor;
        this.entityManager = entityManager;
    }

    protected List<String> clearDynMembers(final DynRealm dynRealm) {
        Query find = entityManager.createNativeQuery(
                "SELECT any_id FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=?");
        find.setParameter(1, dynRealm.getKey());

        @SuppressWarnings("unchecked")
        List<Object> result = find.getResultList();
        List<String> cleared = result.stream().
                map(Object::toString).
                collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        Query delete = entityManager.createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=?");
        delete.setParameter(1, dynRealm.getKey());
        delete.executeUpdate();

        return cleared;
    }

    protected void notifyDynMembershipRemoval(final List<String> anyKeys) {
        anyKeys.forEach(key -> {
            Optional<? extends Any> any = userDAO.findById(key);
            if (any.isEmpty()) {
                any = groupDAO.findById(key);
            }
            if (any.isEmpty()) {
                any = anyObjectDAO.findById(key);
            }
            any.ifPresent(entity -> publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, entity, AuthContextUtils.getDomain())));
        });
    }

    @Override
    public DynRealm saveAndRefreshDynMemberships(final DynRealm dynRealm) {
        DynRealm merged = entityManager.merge(dynRealm);

        // refresh dynamic memberships
        List<String> cleared = clearDynMembers(merged);

        merged.getDynMemberships().stream().map(memb -> searchDAO.search(
                SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()), memb.getAnyType().getKind())).
                forEach(matching -> matching.forEach(any -> {

            Query insert = entityManager.createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
            insert.setParameter(1, any.getKey());
            insert.setParameter(2, merged.getKey());
            insert.executeUpdate();

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, any, AuthContextUtils.getDomain()));
            cleared.remove(any.getKey());
        }));

        notifyDynMembershipRemoval(cleared);

        return merged;
    }

    @Override
    public void deleteById(final String key) {
        DynRealm dynRealm = entityManager.find(JPADynRealm.class, key);
        if (dynRealm == null) {
            return;
        }

        notifyDynMembershipRemoval(clearDynMembers(dynRealm));

        entityManager.remove(dynRealm);
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final Any any) {
        entityManager.createQuery(
                "SELECT e FROM " + JPADynRealm.class.getSimpleName() + " e ", DynRealm.class).getResultStream().
                forEach(dynRealm -> dynRealm.getDynMembership(any.getType()).ifPresent(memb -> {

            boolean matches = anyMatchDAO.matches(
                    any, SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()));

            Query query = entityManager.createNativeQuery(
                    "SELECT COUNT(dynRealm_id) FROM " + DYNMEMB_TABLE + " WHERE any_id=? AND dynRealm_id=?");
            query.setParameter(1, any.getKey());
            query.setParameter(2, dynRealm.getKey());
            boolean existing = ((Number) query.getSingleResult()).longValue() > 0;

            if (matches && !existing) {
                Query insert = entityManager.
                        createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, any.getKey());
                insert.setParameter(2, dynRealm.getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager.createNativeQuery(
                        "DELETE FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=? AND any_id=?");
                delete.setParameter(1, dynRealm.getKey());
                delete.setParameter(2, any.getKey());
                delete.executeUpdate();
            }
        }));
    }

    @Override
    public void removeDynMemberships(final String anyKey) {
        Query delete = entityManager.
                createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, anyKey);
        delete.executeUpdate();
    }
}
