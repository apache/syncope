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
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAuditEvent;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jAuditEventDAO implements AuditEventDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(AuditEventDAO.class);

    protected static class AuditEventCriteriaBuilder {

        protected final StringBuilder query = new StringBuilder();

        protected String andIfNeeded() {
            return query.length() == 0 ? " " : " AND ";
        }

        protected AuditEventCriteriaBuilder entityKey(final String entityKey) {
            if (entityKey != null) {
                query.append(andIfNeeded()).
                        append("(n.before =~ '.*key.*").append(entityKey).append(".*' OR ").
                        append("n.inputs =~ '.*key.*").append(entityKey).append(".*' OR ").
                        append("n.output =~ '.*key.*").append(entityKey).append(".*' OR ").
                        append("n.throwable =~ '.*key.*").append(entityKey).append(".*')");
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
                    append("n.opEvent =~ '").
                    append(OpEvent.toString(type, category, subcategory, op, outcome).
                            replace("[", "\\[").replace("]", "\\]").replace("\\[\\]", "\\[.*\\]")).
                    append("'");

            return this;
        }

        public AuditEventCriteriaBuilder before(final OffsetDateTime before, final Map<String, Object> parameters) {
            if (before != null) {
                query.append(andIfNeeded()).append("n.").append("when").append(" <= $before");
                parameters.put("before", before);
            }
            return this;
        }

        public AuditEventCriteriaBuilder after(final OffsetDateTime after, final Map<String, Object> parameters) {
            if (after != null) {
                query.append(andIfNeeded()).append("n.").append("when").append(" >= $after");
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

    protected final NodeValidator nodeValidator;

    public Neo4jAuditEventDAO(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
        this.nodeValidator = nodeValidator;
    }

    @Transactional
    @Override
    public AuditEvent save(final AuditEvent auditEvent) {
        return neo4jTemplate.save(nodeValidator.validate(auditEvent));
    }

    protected AuditEventCriteriaBuilder criteriaBuilder(final String entityKey) {
        return new AuditEventCriteriaBuilder().entityKey(entityKey);
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

        Map<String, Object> parameters = new HashMap<>();
        String query = "MATCH (n:" + Neo4jAuditEvent.NODE + ") "
                + " WHERE " + criteriaBuilder(entityKey).
                        opEvent(type, category, subcategory, op, outcome).
                        before(before, parameters).
                        after(after, parameters).
                        build()
                + " RETURN COUNT(n)";
        return neo4jTemplate.count(query, parameters);
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

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = new StringBuilder("MATCH (n:" + Neo4jAuditEvent.NODE + ") "
                + "WHERE " + criteriaBuilder(entityKey).
                        opEvent(type, category, subcategory, op, outcome).
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
                map(found -> neo4jTemplate.findById(found.get("n.id"), Neo4jAuditEvent.class)).
                flatMap(Optional::stream).map(this::toAuditEventTO).toList();
    }
}
