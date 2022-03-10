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

import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.jpa.entity.JPAReportExec;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class JPAReportExecDAO extends AbstractDAO<ReportExec> implements ReportExecDAO {

    @Override
    public ReportExec find(final String key) {
        return entityManager().find(JPAReportExec.class, key);
    }

    @Override
    public List<ReportExec> findRecent(final int max) {
        TypedQuery<ReportExec> query = entityManager().createQuery(
                "SELECT e FROM " + JPAReportExec.class.getSimpleName() + " e "
                + "WHERE e.end IS NOT NULL ORDER BY e.end DESC", ReportExec.class);
        query.setMaxResults(max);

        return query.getResultList();
    }

    private ReportExec findLatest(final Report report, final String field) {
        TypedQuery<ReportExec> query = entityManager().createQuery(
                "SELECT e FROM " + JPAReportExec.class.getSimpleName() + " e "
                + "WHERE e.report=:report ORDER BY e." + field + " DESC", ReportExec.class);
        query.setParameter("report", report);
        query.setMaxResults(1);

        List<ReportExec> result = query.getResultList();
        return result == null || result.isEmpty()
                ? null
                : result.iterator().next();
    }

    @Override
    public ReportExec findLatestStarted(final Report report) {
        return findLatest(report, "start");
    }

    @Override
    public ReportExec findLatestEnded(final Report report) {
        return findLatest(report, "end");
    }

    @Override
    public int count(final String reportKey) {
        Query countQuery = entityManager().createNativeQuery(
                "SELECT COUNT(e.id) FROM " + JPAReportExec.TABLE + " e WHERE e.report_id=?1");
        countQuery.setParameter(1, reportKey);

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    private static String toOrderByStatement(final List<OrderByClause> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        for (OrderByClause clause : orderByClauses) {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(JPAReportExec.class, field) != null) {
                statement.append("e.").append(field).append(' ').append(clause.getDirection().name());
            }
        }

        if (statement.length() == 0) {
            statement.append("ORDER BY e.id DESC");
        } else {
            statement.insert(0, "ORDER BY ");
        }
        return statement.toString();
    }

    @Override
    public List<ReportExec> findAll(final Report report,
            final int page, final int itemsPerPage, final List<OrderByClause> orderByClauses) {

        String queryString =
                "SELECT e FROM " + JPAReportExec.class.getSimpleName() + " e WHERE e.report=:report "
                + toOrderByStatement(orderByClauses);

        TypedQuery<ReportExec> query = entityManager().createQuery(queryString, ReportExec.class);
        query.setParameter("report", report);

        // page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public List<ReportExec> findAll(
            final Report report,
            final OffsetDateTime startedBefore,
            final OffsetDateTime startedAfter,
            final OffsetDateTime endedBefore,
            final OffsetDateTime endedAfter) {

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPAReportExec.class.getSimpleName()).
                append(" e WHERE e.report=:report ");

        if (startedBefore != null) {
            queryString.append(" AND e.start < :startedBefore");
        }
        if (startedAfter != null) {
            queryString.append(" AND e.start > :startedAfter");
        }
        if (endedBefore != null) {
            queryString.append(" AND e.end < :endedBefore");
        }
        if (endedAfter != null) {
            queryString.append(" AND e.end > :endedAfter");
        }

        TypedQuery<ReportExec> query = entityManager().createQuery(queryString.toString(), ReportExec.class);
        query.setParameter("report", report);
        if (startedBefore != null) {
            query.setParameter("startedBefore", startedBefore);
        }
        if (startedAfter != null) {
            query.setParameter("startedAfter", startedAfter);
        }
        if (endedBefore != null) {
            query.setParameter("endedBefore", endedBefore);
        }
        if (endedAfter != null) {
            query.setParameter("endedAfter", endedAfter);
        }

        return query.getResultList();
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ReportExec save(final ReportExec execution) {
        return entityManager().merge(execution);
    }

    @Override
    public void delete(final String key) {
        ReportExec execution = find(key);
        if (execution == null) {
            return;
        }

        delete(execution);
    }

    @Override
    public void delete(final ReportExec execution) {
        if (execution.getReport() != null) {
            execution.getReport().getExecs().remove(execution);
        }

        entityManager().remove(execution);
    }
}
