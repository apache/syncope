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

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class JPARealmSearchDAO implements RealmSearchDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(RealmSearchDAO.class);

    protected static int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected static StringBuilder buildDescendantsQuery(
            final Set<String> bases,
            final String keyword,
            final List<Object> parameters) {

        String basesClause = bases.stream().
                map(base -> "e.fullPath=?" + setParameter(parameters, base)
                + " OR e.fullPath LIKE ?" + setParameter(
                        parameters, SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                collect(Collectors.joining(" OR "));

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARealm.class.getSimpleName()).append(" e ").
                append("WHERE (").append(basesClause).append(')');

        if (keyword != null) {
            queryString.append(" AND LOWER(e.name) LIKE ?").
                    append(setParameter(parameters, "%" + keyword.replaceAll("_", "\\\\_").toLowerCase() + "%"));
        }

        return queryString;
    }

    protected final EntityManager entityManager;

    public JPARealmSearchDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Realm> findByFullPath(final String fullPath) {
        if (StringUtils.isBlank(fullPath)
                || (!SyncopeConstants.ROOT_REALM.equals(fullPath)
                && !RealmDAO.PATH_PATTERN.matcher(fullPath).matches())) {

            throw new MalformedPathException(fullPath);
        }

        TypedQuery<Realm> query = entityManager.createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.fullPath=:fullPath", Realm.class);
        query.setParameter("fullPath", fullPath);

        Realm result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("Realm with fullPath {} not found", fullPath, e);
        }

        return Optional.ofNullable(result);
    }

    @Override
    public List<Realm> findByName(final String name) {
        TypedQuery<Realm> query = entityManager.createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.name=:name", Realm.class);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        TypedQuery<Realm> query = entityManager.createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.parent=:realm", Realm.class);
        query.setParameter("realm", realm);

        return query.getResultList();
    }

    @Override
    public long countDescendants(final String base, final String keyword) {
        return countDescendants(Set.of(base), keyword);
    }

    @Override
    public long countDescendants(final Set<String> bases, final String keyword) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, keyword, parameters);
        Query query = entityManager.createQuery(StringUtils.replaceOnce(
                queryString.toString(),
                "SELECT e ",
                "SELECT COUNT(e) "));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<Realm> findDescendants(final String base, final String keyword, final Pageable pageable) {
        return findDescendants(Set.of(base), keyword, pageable);
    }

    @Override
    public List<Realm> findDescendants(final Set<String> bases, final String keyword, final Pageable pageable) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, keyword, parameters);
        TypedQuery<Realm> query = entityManager.createQuery(
                queryString.append(" ORDER BY e.fullPath").toString(), Realm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    @Override
    public List<String> findDescendants(final String base, final String prefix) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(Set.of(base), null, parameters);
        TypedQuery<Realm> query = entityManager.createQuery(queryString.
                append(" AND (e.fullPath=?").
                append(setParameter(parameters, prefix)).
                append(" OR e.fullPath LIKE ?").
                append(setParameter(parameters, SyncopeConstants.ROOT_REALM.equals(prefix) ? "/%" : prefix + "/%")).
                append(')').
                append(" ORDER BY e.fullPath").toString(),
                Realm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return query.getResultList().stream().map(Realm::getKey).toList();
    }
}
