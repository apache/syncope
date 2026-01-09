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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractAnyRepoExt<A extends Any> implements AnyRepoExt<A> {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyRepoExt.class);

    protected final DynRealmDAO dynRealmDAO;

    protected final EntityManager entityManager;

    protected final AnyChecker anyChecker;

    protected final AnyFinder anyFinder;

    protected final AnyUtils anyUtils;

    protected final String table;

    protected AbstractAnyRepoExt(
            final DynRealmDAO dynRealmDAO,
            final EntityManager entityManager,
            final AnyChecker anyChecker,
            final AnyFinder anyFinder,
            final AnyUtils anyUtils) {

        this.dynRealmDAO = dynRealmDAO;
        this.entityManager = entityManager;
        this.anyChecker = anyChecker;
        this.anyFinder = anyFinder;
        this.anyUtils = anyUtils;
        switch (anyUtils.anyTypeKind()) {
            case ANY_OBJECT:
                table = JPAAnyObject.TABLE;
                break;

            case GROUP:
                table = JPAGroup.TABLE;
                break;

            case USER:
            default:
                table = JPAUser.TABLE;
        }
    }

    protected <T> T query(final String sql, final ResultSetExtractor<T> rse, final String... parameters) {
        return entityManager.unwrap(Session.class).doReturningWork(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        stmt.setString(i + 1, parameters[i]);
                    }
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    return rse.extractData(rs);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OffsetDateTime> findLastChange(final String key) {
        return query(
                "SELECT creationDate, lastChangeDate FROM " + table + " WHERE id=?",
                rs -> {
                    if (rs.next()) {
                        OffsetDateTime creationDate = rs.getObject(1, OffsetDateTime.class);
                        OffsetDateTime lastChangeDate = rs.getObject(2, OffsetDateTime.class);
                        return Optional.ofNullable(lastChangeDate).or(() -> Optional.ofNullable(creationDate));
                    }

                    return Optional.empty();
                },
                key);
    }

    protected abstract void securityChecks(A any);

    @SuppressWarnings("unchecked")
    protected Optional<A> findById(final String key) {
        return Optional.ofNullable((A) entityManager.find(anyUtils.anyClass(), key));
    }

    @Transactional(readOnly = true)
    @Override
    public A authFind(final String key) {
        if (key == null) {
            throw new NotFoundException("Null key");
        }

        A any = findById(key).orElseThrow(() -> new NotFoundException(anyUtils.anyTypeKind().name() + ' ' + key));

        securityChecks(any);

        return any;
    }

    @Override
    public List<A> findByDerAttrValue(final String expression, final String value, final boolean ignoreCaseMatch) {
        return anyFinder.findByDerAttrValue(anyUtils.anyTypeKind(), expression, value, ignoreCaseMatch);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> findDynRealms(final String key) {
        return query(
                "SELECT DISTINCT dynRealm_id FROM " + DynRealmRepoExt.DYNMEMB_TABLE + " WHERE any_id=?",
                rs -> {
                    List<String> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(rs.getString(1));
                    }
                    return result;
                },
                key);
    }

    @Override
    public void deleteRelationship(final Relationship<? extends A, AnyObject> relationship) {
        entityManager.remove(relationship);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public void evict(final Class<A> entityClass, final String key) {
        Optional.ofNullable(entityManager.find(entityClass, key)).ifPresent(entityManager::detach);
    }
}
