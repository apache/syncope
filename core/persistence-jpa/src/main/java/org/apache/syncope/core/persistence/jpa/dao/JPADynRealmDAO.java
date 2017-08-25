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
import java.util.Optional;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.jpa.entity.JPADynRealm;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPADynRealmDAO extends AbstractDAO<DynRealm> implements DynRealmDAO {

    public static final String DYNMEMB_TABLE = "DynRealmMembers";

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
        DynRealm merged = entityManager().merge(dynRealm);

        // refresh dynamic memberships
        clearDynMembers(merged);

        merged.getDynMemberships().stream().map(memb -> searchDAO().search(
                SearchCondConverter.convert(memb.getFIQLCond()), memb.getAnyType().getKind())).
                forEachOrdered(matching -> {
                    matching.forEach(any -> {
                        Query insert = entityManager().createNativeQuery("INSERT INTO " + DYNMEMB_TABLE
                                + " VALUES(?, ?)");
                        insert.setParameter(1, any.getKey());
                        insert.setParameter(2, merged.getKey());
                        insert.executeUpdate();

                        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, any, AuthContextUtils.getDomain()));
                    });
                });

        return merged;
    }

    @Override
    public void delete(final String key) {
        DynRealm dynRealm = find(key);
        if (dynRealm == null) {
            return;
        }

        clearDynMembers(dynRealm);

        entityManager().remove(dynRealm);
    }

    @Override
    public void clearDynMembers(final DynRealm dynRealm) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=?");
        delete.setParameter(1, dynRealm.getKey());
        delete.executeUpdate();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final Any<?> any) {
        findAll().forEach(dynRealm -> {
            Optional<? extends DynRealmMembership> memb = dynRealm.getDynMembership(any.getType());
            if (memb.isPresent()) {
                Query delete = entityManager().createNativeQuery(
                        "DELETE FROM " + DYNMEMB_TABLE + " WHERE dynRealm_id=? AND any_id=?");
                delete.setParameter(1, dynRealm.getKey());
                delete.setParameter(2, any.getKey());
                delete.executeUpdate();
                if (searchDAO().matches(any, SearchCondConverter.convert(memb.get().getFIQLCond()))) {
                    Query insert = entityManager().createNativeQuery("INSERT INTO " + DYNMEMB_TABLE + " VALUES(?, ?)");
                    insert.setParameter(1, any.getKey());
                    insert.setParameter(2, dynRealm.getKey());
                    insert.executeUpdate();
                }
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
