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
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.jpa.entity.JPADynRealm;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;

public class JPADynRealmDAO extends AbstractDAO<DynRealm> implements DynRealmDAO {

    public static final String DYNMEMB_TABLE = "DynRealmMembers";

    protected final ApplicationEventPublisher publisher;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO searchDAO;

    protected final AnyMatchDAO anyMatchDAO;

    protected final SearchCondVisitor searchCondVisitor;

    public JPADynRealmDAO(
            final ApplicationEventPublisher publisher,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final AnyMatchDAO anyMatchDAO,
            final SearchCondVisitor searchCondVisitor) {

        this.publisher = publisher;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.searchDAO = searchDAO;
        this.anyMatchDAO = anyMatchDAO;
        this.searchCondVisitor = searchCondVisitor;
    }

    @Override
    public DynRealm find(final String key) {
        return entityManager().find(JPADynRealm.class, key);
    }

    @Override
    public List<DynRealm> findAll() {
        TypedQuery<DynRealm> query = entityManager().createQuery(
                "SELECT e FROM " + JPADynRealm.class.getSimpleName() + " e ", DynRealm.class);
        return query.getResultList();
    }

    @Override
    public DynRealm save(final DynRealm dynRealm) {
        return entityManager().merge(dynRealm);
    }

    @SuppressWarnings("unchecked")
    protected List<String> clearDynMembers(final DynRealm dynRealm) {
        Query find = entityManager().createNativeQuery(
                "SELECT any_id FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=?");
        find.setParameter(1, dynRealm.getKey());

        List<String> cleared = new ArrayList<>();
        find.getResultList().stream().map(key -> key instanceof Object[]
                ? (String) ((Object[]) key)[0]
                : ((String) key)).
                forEach(key -> cleared.add((String) key));

        Query delete = entityManager().createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=?");
        delete.setParameter(1, dynRealm.getKey());
        delete.executeUpdate();

        return cleared;
    }

    protected void notifyDynMembershipRemoval(final List<String> anyKeys) {
        anyKeys.forEach(key -> {
            Any<?> any = userDAO.find(key);
            if (any == null) {
                any = groupDAO.find(key);
            }
            if (any == null) {
                any = anyObjectDAO.find(key);
            }
            if (any != null) {
                publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, any, AuthContextUtils.getDomain()));
            }
        });
    }

    @Override
    public DynRealm saveAndRefreshDynMemberships(final DynRealm dynRealm) {
        DynRealm merged = save(dynRealm);

        // refresh dynamic memberships
        List<String> cleared = clearDynMembers(merged);

        merged.getDynMemberships().stream().map(memb -> searchDAO.search(
                SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()), memb.getAnyType().getKind())).
                forEach(matching -> matching.forEach(any -> {

            Query insert = entityManager().createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
            insert.setParameter(1, any.getKey());
            insert.setParameter(2, merged.getKey());
            insert.executeUpdate();

            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, any, AuthContextUtils.getDomain()));
            cleared.remove(any.getKey());
        }));

        notifyDynMembershipRemoval(cleared);

        return merged;
    }

    @Override
    public void delete(final String key) {
        DynRealm dynRealm = find(key);
        if (dynRealm == null) {
            return;
        }

        notifyDynMembershipRemoval(clearDynMembers(dynRealm));

        entityManager().remove(dynRealm);
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final Any<?> any) {
        findAll().forEach(dynRealm -> dynRealm.getDynMembership(any.getType()).ifPresent(memb -> {
            boolean matches = anyMatchDAO.matches(
                    any, SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()));

            Query find = entityManager().createNativeQuery(
                    "SELECT dynRealm_id FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
            find.setParameter(1, any.getKey());
            boolean existing = !find.getResultList().isEmpty();

            if (matches && !existing) {
                Query insert = entityManager().
                        createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, any.getKey());
                insert.setParameter(2, dynRealm.getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager().createNativeQuery(
                        "DELETE FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=? AND any_id=?");
                delete.setParameter(1, dynRealm.getKey());
                delete.setParameter(2, any.getKey());
                delete.executeUpdate();
            }
        }));
    }

    @Override
    public void removeDynMemberships(final String anyKey) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, anyKey);
        delete.executeUpdate();
    }
}
