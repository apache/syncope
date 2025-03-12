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
import org.apache.commons.text.TextStringBuilder;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRemediation;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPullTask;
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

    public RemediationRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(neo4jTemplate, neo4jClient);
    }

    @Override
    public List<Remediation> findByAnyType(final AnyType anyType) {
        return findByRelationship(
                Neo4jRemediation.NODE, Neo4jAnyType.NODE, anyType.getKey(), Neo4jRemediation.class, null);
    }

    @Override
    public List<Remediation> findByPullTask(final PullTask pullTask) {
        return findByRelationship(
                Neo4jRemediation.NODE, Neo4jPullTask.NODE, pullTask.getKey(), Neo4jRemediation.class, null);
    }

    protected TextStringBuilder query(
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Map<String, Object> parameters) {

        TextStringBuilder query = new TextStringBuilder("MATCH (n:").append(Neo4jRemediation.NODE).append(") ");

        List<String> conditions = new ArrayList<>();
        if (before != null) {
            conditions.add("n.instant <= $before");
            parameters.put("before", before);
        }
        if (after != null) {
            conditions.add("n.instant >= $after");
            parameters.put("after", after);
        }

        if (!conditions.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", conditions));
        }

        return query;
    }

    @Override
    public long count(final OffsetDateTime before, final OffsetDateTime after) {
        Map<String, Object> parameters = new HashMap<>();

        TextStringBuilder query = query(before, after, parameters).append(" RETURN COUNT(n)");
        return neo4jTemplate.count(query.toString(), parameters);
    }

    @Override
    public List<Remediation> findAll(
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        Map<String, Object> parameters = new HashMap<>();

        TextStringBuilder query = query(before, after, parameters).append(" RETURN n.id");

        if (!pageable.getSort().isEmpty()) {
            query.append(" ORDER BY ");
            pageable.getSort().forEach(clause -> {
                String field = clause.getProperty().trim();
                boolean ack = true;
                if ("resource".equals(field)) {
                    query.replace(
                            StringMatcherFactory.INSTANCE.stringMatcher("MATCH (n:" + Neo4jRemediation.NODE + ") "),
                            "MATCH (n:" + Neo4jRemediation.NODE + ")-[]-"
                            + "(:" + Neo4jPullTask.NODE + ")-[]-"
                            + "(r:" + Neo4jExternalResource.NODE + ")",
                            0,
                            query.length(),
                            1);
                    query.append("r.id");
                } else {
                    Field beanField = ReflectionUtils.findField(Neo4jRemediation.class, field);
                    if (beanField == null) {
                        ack = false;
                        LOG.warn("Remediation sort request by {}: unsupported, ignoring", field);
                    } else {
                        query.append("n.").append(field);
                    }
                }
                if (ack) {
                    if (clause.getDirection() == Sort.Direction.ASC) {
                        query.append(" ASC");
                    } else {
                        query.append(" DESC");
                    }
                    query.append(',');
                }
            });

            query.deleteCharAt(query.length() - 1);
        }

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(
                query.toString()).bindAll(parameters).fetch().all(), "n.id", Neo4jRemediation.class, null);
    }

    @Transactional
    @Override
    public void deleteById(final String key) {
        neo4jTemplate.deleteById(key, Neo4jRemediation.class);
    }
}
