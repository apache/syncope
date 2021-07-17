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

import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.jpa.entity.JPAAuditConf;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;

public class JPAAuditConfDAO extends AbstractDAO<AuditConf> implements AuditConfDAO {

    protected static class MessageCriteriaBuilder {

        protected final StringBuilder query = new StringBuilder();

        protected String andIfNeeded() {
            return query.length() == 0 ? " " : " AND ";
        }

        protected MessageCriteriaBuilder entityKey(final String entityKey) {
            if (entityKey != null) {
                query.append(andIfNeeded()).append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%key%").append(entityKey).append("%'");
            }
            return this;
        }

        public MessageCriteriaBuilder type(final AuditElements.EventCategoryType type) {
            if (type != null) {
                query.append(andIfNeeded()).append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"type\":\"").append(type.name()).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder category(final String category) {
            if (StringUtils.isNotBlank(category)) {
                query.append(andIfNeeded()).append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"category\":\"").append(category).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder subcategory(final String subcategory) {
            if (StringUtils.isNotBlank(subcategory)) {
                query.append(andIfNeeded()).append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"subcategory\":\"").append(subcategory).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder events(final List<String> events) {
            if (!events.isEmpty()) {
                query.append(andIfNeeded()).append("( ").
                        append(events.stream().
                                map(event -> AUDIT_ENTRY_MESSAGE_COLUMN + " LIKE '%\"event\":\"" + event + "\"%'").
                                collect(Collectors.joining(" OR "))).
                        append(" )");
            }
            return this;
        }

        public MessageCriteriaBuilder result(final AuditElements.Result result) {
            if (result != null) {
                query.append(andIfNeeded()).append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"result\":\"").append(result.name()).append("\"%' ");
            }
            return this;
        }

        public String build() {
            return query.toString();
        }
    }

    @Override
    public AuditConf find(final String key) {
        return entityManager().find(JPAAuditConf.class, key);
    }

    @Override
    public List<AuditConf> findAll() {
        TypedQuery<AuditConf> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuditConf.class.getSimpleName() + " e ", AuditConf.class);
        return query.getResultList();
    }

    @Override
    public AuditConf save(final AuditConf auditConf) {
        return entityManager().merge(auditConf);
    }

    @Override
    public void delete(final AuditConf auditConf) {
        entityManager().remove(auditConf);
    }

    protected MessageCriteriaBuilder messageCriteriaBuilder(final String entityKey) {
        return new MessageCriteriaBuilder().entityKey(entityKey);
    }

    @Override
    public int countEntries(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result) {

        String queryString = "SELECT COUNT(0)"
                + " FROM " + AUDIT_ENTRY_TABLE
                + " WHERE " + messageCriteriaBuilder(entityKey).
                        type(type).
                        category(category).
                        subcategory(subcategory).
                        result(result).
                        events(events).
                        build();
        Query countQuery = entityManager().createNativeQuery(queryString);

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    protected String select() {
        return AUDIT_ENTRY_MESSAGE_COLUMN;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuditEntry> searchEntries(
            final String entityKey,
            final int page,
            final int itemsPerPage,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final List<OrderByClause> orderByClauses) {

        String queryString = "SELECT " + select()
                + " FROM " + AUDIT_ENTRY_TABLE
                + " WHERE " + messageCriteriaBuilder(entityKey).
                        type(type).
                        category(category).
                        subcategory(subcategory).
                        result(result).
                        events(events).
                        build();
        if (!orderByClauses.isEmpty()) {
            queryString += " ORDER BY " + orderByClauses.stream().
                    map(orderBy -> orderBy.getField() + ' ' + orderBy.getDirection().name()).
                    collect(Collectors.joining(","));
        }

        Query query = entityManager().createNativeQuery(queryString);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        @SuppressWarnings("unchecked")
        List<Object> entries = query.getResultList();
        return entries.stream().map(row -> {
            String value;
            if (row instanceof Clob) {
                Clob clob = (Clob) row;
                try {
                    value = clob.getSubString(1, (int) clob.length());
                } catch (SQLException e) {
                    LOG.error("Unexpected error reading Audit Entry for entity key {}", entityKey, e);
                    return null;
                }
            } else {
                value = row.toString();
            }
            return POJOHelper.deserialize(value, AuditEntry.class);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
