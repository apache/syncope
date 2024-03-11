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
import jakarta.persistence.Query;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.jpa.entity.JPAAuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class JPAAuditEventDAO implements AuditEventDAO {

    protected static class AuditEventCriteriaBuilder {

        protected final StringBuilder query = new StringBuilder();

        protected String andIfNeeded() {
            return query.length() == 0 ? " " : " AND ";
        }

        protected int setParameter(final List<Object> parameters, final Object parameter) {
            parameters.add(parameter);
            return parameters.size();
        }

        protected AuditEventCriteriaBuilder entityKey(final String entityKey) {
            if (entityKey != null) {
                query.append(andIfNeeded()).
                        append("(before_value LIKE '%key%").append(entityKey).append("%' OR ").
                        append("inputs LIKE '%key%").append(entityKey).append("%' OR ").
                        append("output LIKE '%key%").append(entityKey).append("%' OR ").
                        append("throwable LIKE '%key%").append(entityKey).append("%')");
            }
            return this;
        }

        public AuditEventCriteriaBuilder opEvent(
                final OpEvent.CategoryType type,
                final String category,
                final String subcategory,
                final String op,
                final OpEvent.Outcome outcome) {

            query.append(andIfNeeded()).
                    append("opEvent LIKE '").
                    append(OpEvent.toString(type, category, subcategory, op, outcome).replace("[]", "[%]")).
                    append("'");

            return this;
        }

        public AuditEventCriteriaBuilder before(final OffsetDateTime before, final List<Object> parameters) {
            if (before != null) {
                query.append(andIfNeeded()).
                        append("event_date").append(" <= ?").append(setParameter(parameters, before));
            }
            return this;
        }

        public AuditEventCriteriaBuilder after(final OffsetDateTime after, final List<Object> parameters) {
            if (after != null) {
                query.append(andIfNeeded()).
                        append("event_date").append(" >= ?").append(setParameter(parameters, after));
            }
            return this;
        }

        public String build() {
            return query.toString();
        }
    }

    protected final EntityManager entityManager;

    public JPAAuditEventDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    @Override
    public AuditEvent save(final AuditEvent auditEvent) {
        return entityManager.merge(auditEvent);
    }

    protected AuditEventCriteriaBuilder criteriaBuilder(final String entityKey) {
        return new AuditEventCriteriaBuilder().entityKey(entityKey);
    }

    protected void fillWithParameters(final Query query, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof Boolean aBoolean) {
                query.setParameter(i + 1, aBoolean ? 1 : 0);
            } else {
                query.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public long count(
            final String entityKey,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        List<Object> parameters = new ArrayList<>();
        String queryString = "SELECT COUNT(0)"
                + " FROM " + JPAAuditEvent.TABLE
                + " WHERE" + criteriaBuilder(entityKey).
                        opEvent(type, category, subcategory, op, outcome).
                        before(before, parameters).
                        after(after, parameters).
                        build();
        Query query = entityManager.createNativeQuery(queryString);
        fillWithParameters(query, parameters);

        return ((Number) query.getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuditEventTO> search(
            final String entityKey,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        List<Object> parameters = new ArrayList<>();
        String queryString = "SELECT id"
                + " FROM " + JPAAuditEvent.TABLE
                + " WHERE" + criteriaBuilder(entityKey).
                        opEvent(type, category, subcategory, op, outcome).
                        before(before, parameters).
                        after(after, parameters).
                        build();
        if (!pageable.getSort().isEmpty()) {
            queryString += " ORDER BY " + pageable.getSort().stream().
                    map(clause -> ("when".equals(clause.getProperty()) ? "event_date" : clause.getProperty())
                    + ' ' + clause.getDirection().name()).
                    collect(Collectors.joining(","));
        }

        Query query = entityManager.createNativeQuery(queryString);
        fillWithParameters(query, parameters);

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Object> entries = query.getResultList();
        return entries.stream().
                map(row -> entityManager.find(JPAAuditEvent.class, row.toString())).
                filter(Objects::nonNull).map(this::toAuditEventTO).toList();
    }
}
