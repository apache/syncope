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
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class JPATaskExecDAO extends AbstractDAO<TaskExec> implements TaskExecDAO {

    protected final TaskDAO taskDAO;

    public JPATaskExecDAO(final TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    @Override
    public TaskExec find(final String key) {
        return entityManager().find(JPATaskExec.class, key);
    }

    @Override
    public List<TaskExec> findRecent(final int max) {
        TypedQuery<TaskExec> query = entityManager().createQuery(
                "SELECT e FROM " + JPATaskExec.class.getSimpleName() + " e "
                + "WHERE e.end IS NOT NULL ORDER BY e.end DESC", TaskExec.class);
        query.setMaxResults(max);

        return query.getResultList();
    }

    protected <T extends Task> TaskExec findLatest(final T task, final String field) {
        TypedQuery<TaskExec> query = entityManager().createQuery(
                "SELECT e FROM " + JPATaskExec.class.getSimpleName() + " e "
                + "WHERE e.task=:task ORDER BY e." + field + " DESC", TaskExec.class);
        query.setParameter("task", task);
        query.setMaxResults(1);

        List<TaskExec> result = query.getResultList();
        return result == null || result.isEmpty()
                ? null
                : result.iterator().next();
    }

    @Override
    public <T extends Task> TaskExec findLatestStarted(final T task) {
        return findLatest(task, "start");
    }

    @Override
    public <T extends Task> TaskExec findLatestEnded(final T task) {
        return findLatest(task, "end");
    }

    @Override
    public <T extends Task> List<TaskExec> findAll(
            final T task,
            final OffsetDateTime startedBefore,
            final OffsetDateTime startedAfter,
            final OffsetDateTime endedBefore,
            final OffsetDateTime endedAfter) {

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPATaskExec.class.getSimpleName()).
                append(" e WHERE e.task=:task ");

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

        TypedQuery<TaskExec> query = entityManager().createQuery(queryString.toString(), TaskExec.class);
        query.setParameter("task", task);
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

    @Override
    public int count(final String taskKey) {
        Query countQuery = entityManager().createNativeQuery(
                "SELECT COUNT(e.id) FROM " + JPATaskExec.TABLE + " e WHERE e.task_id=?1");
        countQuery.setParameter(1, taskKey);

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    protected String toOrderByStatement(final List<OrderByClause> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(JPATaskExec.class, field) != null) {
                statement.append("e.").append(field).append(' ').append(clause.getDirection().name());
            }
        });

        if (statement.length() == 0) {
            statement.append("ORDER BY e.id DESC");
        } else {
            statement.insert(0, "ORDER BY ");
        }
        return statement.toString();
    }

    @Override
    public <T extends Task> List<TaskExec> findAll(
            final T task, final int page, final int itemsPerPage, final List<OrderByClause> orderByClauses) {

        String queryString =
                "SELECT e FROM " + JPATaskExec.class.getSimpleName() + " e WHERE e.task=:task "
                + toOrderByStatement(orderByClauses);

        TypedQuery<TaskExec> query = entityManager().createQuery(queryString, TaskExec.class);
        query.setParameter("task", task);

        // page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public TaskExec save(final TaskExec execution) {
        return entityManager().merge(execution);
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public void saveAndAdd(final String taskKey, final TaskExec execution) {
        Task task = taskDAO.find(taskKey);
        task.add(execution);
        taskDAO.save(task);
    }

    @Override
    public void delete(final String key) {
        TaskExec execution = find(key);
        if (execution == null) {
            return;
        }

        delete(execution);
    }

    @Override
    public void delete(final TaskExec execution) {
        if (execution.getTask() != null) {
            execution.getTask().getExecs().remove(execution);
        }

        entityManager().remove(execution);
    }
}
