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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jReport;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jReportExec;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.util.ReflectionUtils;

public class ReportExecRepoExtImpl extends AbstractDAO implements ReportExecRepoExt {

    protected final NodeValidator nodeValidator;

    public ReportExecRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
    }

    @Override
    public List<ReportExec> findRecent(final int max) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jReportExec.NODE + ")-"
                + "[:" + Neo4jReport.REPORT_EXEC_REL + "]-"
                + "(p:" + Neo4jReport.NODE + " {id: $id}) "
                + "WHERE n.endDate IS NOT NULL "
                + "RETURN n.id ORDER BY n.end DateDESC LIMIT " + max).fetch().all(),
                "n.id",
                Neo4jReportExec.class,
                null);
    }

    protected ReportExec findLatest(final Report report, final String field) {
        return neo4jClient.query(
                "MATCH (n:" + Neo4jReportExec.NODE + ")-"
                + "[:" + Neo4jReport.REPORT_EXEC_REL + "]-"
                + "(p:" + Neo4jReport.NODE + " {id: $id}) "
                + "RETURN n.id ORDER BY n." + field + " DESC LIMIT 1").
                bindAll(Map.of("id", report.getKey())).fetch().one().
                flatMap(super.<ReportExec, Neo4jReportExec>toOptional("n.id", Neo4jReportExec.class, null)).
                orElse(null);
    }

    @Override
    public ReportExec findLatestStarted(final Report report) {
        return findLatest(report, "startDate");
    }

    @Override
    public ReportExec findLatestEnded(final Report report) {
        return findLatest(report, "endDate");
    }

    protected StringBuilder query(
            final Report report,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Map<String, Object> parameters) {

        parameters.put("id", report.getKey());

        StringBuilder query = new StringBuilder(
                "MATCH (n:").append(Neo4jReportExec.NODE).append(")-").
                append("[:").append(Neo4jReport.REPORT_EXEC_REL).append("]-").
                append("(p:").append(Neo4jReport.NODE).append(" {id: $id}) WHERE 1=1 ");

        if (before != null) {
            query.append("AND n.startDate <= $before ");
            parameters.put("before", before);
        }
        if (after != null) {
            query.append("AND n.startDate >= $after ");
            parameters.put("after", after);
        }

        return query;
    }

    @Override
    public long count(
            final Report report,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = query(report, before, after, parameters).append(" RETURN COUNT(n)");
        return neo4jTemplate.count(queryString.toString(), parameters);
    }

    protected String toOrderByStatement(final Stream<Sort.Order> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getProperty().trim();
            if (ReflectionUtils.findField(Neo4jReportExec.class, field) != null) {
                statement.append("n.").append(field).append(' ').append(clause.getDirection().name());
            }
        });

        if (statement.length() == 0) {
            statement.append("ORDER BY n.id DESC");
        } else {
            statement.insert(0, "ORDER BY ");
        }
        return statement.toString();
    }

    @Override
    public List<ReportExec> findAll(
            final Report report,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = query(report, before, after, parameters).
                append(" RETURN n.id ").append(toOrderByStatement(pageable.getSort().stream()));
        if (pageable.isPaged()) {
            queryString.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(
                queryString.toString()).bindAll(parameters).fetch().all(), "n.id", Neo4jReportExec.class, null);
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jReportExec.class).ifPresent(this::delete);
    }

    @Override
    public void delete(final ReportExec execution) {
        Optional.ofNullable(execution.getReport()).ifPresent(report -> report.getExecs().remove(execution));
        neo4jTemplate.deleteById(execution.getKey(), Neo4jReportExec.class);
    }
}
