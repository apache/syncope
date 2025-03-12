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
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.jpa.entity.JPAReportExec;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.ReflectionUtils;

public class ReportExecRepoExtImpl implements ReportExecRepoExt {

    protected final EntityManager entityManager;

    public ReportExecRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ReportExec> findRecent(final int max) {
        TypedQuery<ReportExec> query = entityManager.createQuery(
                "SELECT e FROM " + JPAReportExec.class.getSimpleName() + " e "
                + "WHERE e.end IS NOT NULL ORDER BY e.end DESC", ReportExec.class);
        query.setMaxResults(max);

        return query.getResultList();
    }

    protected ReportExec findLatest(final Report report, final String field) {
        TypedQuery<ReportExec> query = entityManager.createQuery(
                "SELECT e FROM " + JPAReportExec.class.getSimpleName() + " e "
                + "WHERE e.report=:report ORDER BY e." + field + " DESC", ReportExec.class);
        query.setParameter("report", report);
        query.setMaxResults(1);

        List<ReportExec> result = query.getResultList();
        return result == null || result.isEmpty()
                ? null
                : result.getFirst();
    }

    @Override
    public ReportExec findLatestStarted(final Report report) {
        return findLatest(report, "start");
    }

    @Override
    public ReportExec findLatestEnded(final Report report) {
        return findLatest(report, "end");
    }

    protected StringBuilder query(
            final StringBuilder select,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        StringBuilder query = select.
                append(JPAReportExec.class.getSimpleName()).
                append(" e WHERE e.report=:report ");
        if (before != null) {
            query.append("AND e.start <= :before ");
        }
        if (after != null) {
            query.append("AND e.start >= :after ");
        }
        return query;
    }

    @Override
    public long count(
            final Report report,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        StringBuilder queryString = query(new StringBuilder("SELECT COUNT(e) FROM "), before, after);

        Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("report", report);
        if (before != null) {
            query.setParameter("before", before);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    protected String toOrderByStatement(final Stream<Sort.Order> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getProperty().trim();
            if (ReflectionUtils.findField(JPAReportExec.class, field) != null) {
                statement.append("e.").append(field).append(' ').append(clause.getDirection().name());
            }
        });

        if (statement.length() == 0) {
            statement.append(" ORDER BY e.id DESC");
        } else {
            statement.insert(0, " ORDER BY ");
        }
        return statement.toString();
    }

    @Override
    public List<ReportExec> findAll(
            final Report report,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        StringBuilder queryString = query(new StringBuilder("SELECT e FROM "), before, after).
                append(toOrderByStatement(pageable.getSort().stream()));

        TypedQuery<ReportExec> query = entityManager.createQuery(queryString.toString(), ReportExec.class);
        query.setParameter("report", report);
        if (before != null) {
            query.setParameter("before", before);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    @Override
    public void deleteById(final String key) {
        Optional.ofNullable(entityManager.find(JPAReportExec.class, key)).ifPresent(this::delete);
    }

    @Override
    public void delete(final ReportExec execution) {
        Optional.ofNullable(execution.getReport()).ifPresent(report -> report.getExecs().remove(execution));

        entityManager.remove(execution);
    }
}
