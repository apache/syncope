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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.AuditEntryDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jAuditEntryDAO implements AuditEntryDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(AuditEntryDAO.class);

    protected static class MessageCriteriaBuilder {

        protected final StringBuilder query = new StringBuilder();

        protected String andIfNeeded() {
            return query.length() == 0 ? " " : " AND ";
        }

        protected MessageCriteriaBuilder entityKey(final String entityKey) {
            if (entityKey != null) {
                query.append(andIfNeeded()).append("n.").append(MESSAGE_COLUMN).
                        append(" =~ '.*key.*").append(entityKey).append(".*'");
            }
            return this;
        }

        public MessageCriteriaBuilder type(final AuditElements.EventCategoryType type) {
            if (type != null) {
                query.append(andIfNeeded()).append("n.").append(MESSAGE_COLUMN).
                        append(" =~ '.*\"type\":\"").append(type.name()).append("\".*'");
            }
            return this;
        }

        public MessageCriteriaBuilder category(final String category) {
            if (StringUtils.isNotBlank(category)) {
                query.append(andIfNeeded()).append("n.").append(MESSAGE_COLUMN).
                        append(" =~ '.*\"category\":\"").append(category).append("\".*'");
            }
            return this;
        }

        public MessageCriteriaBuilder subcategory(final String subcategory) {
            if (StringUtils.isNotBlank(subcategory)) {
                query.append(andIfNeeded()).append("n.").append(MESSAGE_COLUMN).
                        append(" =~ '.*\"subcategory\":\"").append(subcategory).append("\".*'");
            }
            return this;
        }

        public MessageCriteriaBuilder events(final List<String> events) {
            if (!events.isEmpty()) {
                query.append(andIfNeeded()).append("( ").
                        append(events.stream().
                                map(event -> "n.*" + MESSAGE_COLUMN
                                + " =~ '.*\"event\":\"" + event + "\".*'").
                                collect(Collectors.joining(" OR "))).
                        append(" )");
            }
            return this;
        }

        public MessageCriteriaBuilder result(final AuditElements.Result result) {
            if (result != null) {
                query.append(andIfNeeded()).append("n.").append(MESSAGE_COLUMN).
                        append(" =~ '.*\"result\":\"").append(result.name()).append("\".*' ");
            }
            return this;
        }

        public MessageCriteriaBuilder before(final OffsetDateTime before, final Map<String, Object> parameters) {
            if (before != null) {
                query.append(andIfNeeded()).append("n.").append(EVENT_DATE_COLUMN).
                        append(" <= $before");
                parameters.put("before", before);
            }
            return this;
        }

        public MessageCriteriaBuilder after(final OffsetDateTime after, final Map<String, Object> parameters) {
            if (after != null) {
                query.append(andIfNeeded()).append("n.").append(EVENT_DATE_COLUMN).
                        append(" >= $after");
                parameters.put("after", after);
            }
            return this;
        }

        public String build() {
            return query.toString();
        }
    }

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    public Neo4jAuditEntryDAO(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
    }

    protected MessageCriteriaBuilder messageCriteriaBuilder(final String entityKey) {
        return new MessageCriteriaBuilder().entityKey(entityKey);
    }

    @Override
    public long count(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        Map<String, Object> parameters = new HashMap<>();
        String query = "MATCH (n:" + Neo4jAuditEntry.NODE + ") "
                + " WHERE " + messageCriteriaBuilder(entityKey).
                        type(type).
                        category(category).
                        subcategory(subcategory).
                        result(result).
                        events(events).
                        before(before, parameters).
                        after(after, parameters).
                        build()
                + " RETURN COUNT(n)";
        return neo4jTemplate.count(query, parameters);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuditEntry> search(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = new StringBuilder("MATCH (n:" + Neo4jAuditEntry.NODE + ") "
                + " WHERE " + messageCriteriaBuilder(entityKey).
                        type(type).
                        category(category).
                        subcategory(subcategory).
                        result(result).
                        events(events).
                        before(before, parameters).
                        after(after, parameters).
                        build()
                + " RETURN n.id");

        if (!pageable.getSort().isEmpty()) {
            query.append(" ORDER BY ").append(pageable.getSort().stream().
                    map(clause -> "n." + clause.getProperty() + ' ' + clause.getDirection().name()).
                    collect(Collectors.joining(",")));
        }

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return neo4jClient.query(query.toString()).
                bindAll(parameters).fetch().all().stream().
                map(found -> neo4jTemplate.findById(found.get("n.id"), Neo4jAuditEntry.class)).
                filter(Optional::isPresent).map(Optional::get).map(AuditEntry.class::cast).toList();
    }
}
