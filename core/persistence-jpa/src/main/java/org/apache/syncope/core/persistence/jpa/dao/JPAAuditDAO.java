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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.AuditDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;
import org.apache.syncope.core.provisioning.api.AuditEntryImpl;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.transaction.annotation.Transactional;

public class JPAAuditDAO extends AbstractDAO<AbstractEntity> implements AuditDAO {

    protected static class MessageCriteriaBuilder {

        protected final StringBuilder query = new StringBuilder();

        protected MessageCriteriaBuilder entityKey(final String entityKey) {
            query.append(' ').append(MESSAGE_COLUMN).append(" LIKE '%\"key\":\"").append(entityKey).append("\"%'");
            return this;
        }

        public MessageCriteriaBuilder type(final AuditElements.EventCategoryType type) {
            if (type != null) {
                query.append(" AND " + MESSAGE_COLUMN + " LIKE '%\"type\":\"").append(type.name()).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder category(final String category) {
            if (StringUtils.isNotBlank(category)) {
                query.append(" AND " + MESSAGE_COLUMN + " LIKE '%\"category\":\"").append(category).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder subcategory(final String subcategory) {
            if (StringUtils.isNotBlank(subcategory)) {
                query.append(" AND " + MESSAGE_COLUMN + " LIKE '%\"subcategory\":\"").
                        append(subcategory).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder events(final List<String> events) {
            if (!events.isEmpty()) {
                query.append(" AND ( ").
                        append(events.stream().
                                map(event -> MESSAGE_COLUMN + " LIKE '%\"event\":\"" + event + "\"%'").
                                collect(Collectors.joining(" OR "))).
                        append(" )");
            }
            return this;
        }

        public MessageCriteriaBuilder result(final AuditElements.Result result) {
            if (result != null) {
                query.append(" AND ").
                        append(MESSAGE_COLUMN).append(" LIKE '%\"result\":\"").append(result.name()).append("\"%' ");
            }
            return this;
        }

        public String build() {
            return query.toString();
        }
    }

    protected MessageCriteriaBuilder messageCriteriaBuilder(final String entityKey) {
        return new MessageCriteriaBuilder().entityKey(entityKey);
    }

    protected String select() {
        return MESSAGE_COLUMN;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuditEntry> findByEntityKey(
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
                + " FROM " + TABLE
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
            return POJOHelper.deserialize(value, AuditEntryImpl.class);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public int count(final String key) {
        String queryString = "SELECT COUNT(0) FROM " + TABLE
                + " WHERE " + messageCriteriaBuilder(key).build();
        Query countQuery = entityManager().createNativeQuery(queryString);

        return ((Number) countQuery.getSingleResult()).intValue();
    }
}
