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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRemediation;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPullTask;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class RemediationRepoExtImpl extends AbstractDAO implements RemediationRepoExt {

    protected static final Logger LOG = LoggerFactory.getLogger(RemediationRepoExt.class);

    protected final NodeValidator nodeValidator;

    public RemediationRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
    }

    @Override
    public List<Remediation> findByAnyType(final AnyType anyType) {
        return findByRelationship(Neo4jRemediation.NODE, Neo4jAnyType.NODE, anyType.getKey(), Neo4jRemediation.class);
    }

    @Override
    public List<Remediation> findByPullTask(final PullTask pullTask) {
        return findByRelationship(Neo4jRemediation.NODE, Neo4jPullTask.NODE, pullTask.getKey(), Neo4jRemediation.class);
    }

    protected StringBuilder query(
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Map<String, Object> parameters) {

        StringBuilder query = new StringBuilder("MATCH (n:").append(Neo4jRemediation.NODE).
                append(")-[r]-(e:").append(Neo4jExternalResource.NODE).append(")");

        List<String> conditions = new ArrayList<>();
        if (before != null) {
            conditions.add("e.instant <= $before");
            parameters.put("before", before);
        }
        if (after != null) {
            conditions.add("AND e.instant >= $after ");
            parameters.put("after", after);
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(conditions.stream().collect(Collectors.joining(" AND ")));
        }

        return query;
    }

    @Override
    public long count(final OffsetDateTime before, final OffsetDateTime after) {
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = query(before, after, parameters).append(" RETURN COUNT(n)");
        return neo4jTemplate.count(queryString.toString(), parameters);
    }

    @Override
    public List<Remediation> findAll(
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = query(before, after, parameters).append(" RETURN n.id ");

        if (!pageable.getSort().isEmpty()) {
            queryString.append(" ORDER BY ");
            pageable.getSort().forEach(clause -> {
                String field = clause.getProperty().trim();
                boolean ack = true;
                if ("resource".equals(field)) {
                    queryString.append("e.id");
                } else {
                    Field beanField = ReflectionUtils.findField(Neo4jRemediation.class, field);
                    if (beanField == null) {
                        ack = false;
                        LOG.warn("Remediation sort request by {}: unsupported, ignoring", field);
                    } else {
                        queryString.append("n.").append(field);
                    }
                }
                if (ack) {
                    if (clause.getDirection() == Sort.Direction.ASC) {
                        queryString.append(" ASC");
                    } else {
                        queryString.append(" DESC");
                    }
                    queryString.append(',');
                }
            });

            queryString.deleteCharAt(queryString.length() - 1);
        }

        if (pageable.isPaged()) {
            queryString.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(
                queryString.toString()).bindAll(parameters).fetch().all(), "n.id", Neo4jRemediation.class);
    }

    @Transactional
    @Override
    public void deleteById(final String key) {
        neo4jTemplate.deleteById(key, Neo4jRemediation.class);
    }
}
