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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.Query;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.task.AbstractTaskExec;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class JPATaskExecDAO extends AbstractDAO<TaskExec<?>> implements TaskExecDAO {

    protected final TaskDAO taskDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    public JPATaskExecDAO(final TaskDAO taskDAO, final TaskUtilsFactory taskUtilsFactory) {
        this.taskDAO = taskDAO;
        this.taskUtilsFactory = taskUtilsFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task<T>> TaskExec<T> find(final TaskType type, final String key) {
        return (TaskExec<T>) entityManager().find(taskUtilsFactory.getInstance(type).getTaskExecEntity(), key);
    }

    @Override
    public Optional<TaskExec<?>> find(final String key) {
        TaskExec<?> task = find(TaskType.SCHEDULED, key);
        if (task == null) {
            task = find(TaskType.PULL, key);
        }
        if (task == null) {
            task = find(TaskType.PUSH, key);
        }
        if (task == null) {
            task = find(TaskType.MACRO, key);
        }
        if (task == null) {
            task = find(TaskType.PROPAGATION, key);
        }
        if (task == null) {
            task = find(TaskType.NOTIFICATION, key);
        }

        return Optional.ofNullable(task);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Task<T>> List<TaskExec<T>> findRecent(final TaskType type, final int max) {
        Query query = entityManager().createQuery(
                "SELECT e FROM " + taskUtilsFactory.getInstance(type).getTaskExecEntity().getSimpleName() + " e "
                + "WHERE e.end IS NOT NULL ORDER BY e.end DESC");
        query.setMaxResults(max);

        List<Object> result = query.getResultList();
        return result.stream().map(e -> (TaskExec<T>) e).collect(Collectors.toList());
    }

    @Override
    public List<TaskExec<?>> findRecent(final int max) {
        List<TaskExec<?>> recent = new ArrayList<>();

        for (TaskType taskType : TaskType.values()) {
            recent.addAll(findRecent(taskType, max));
        }

        return recent.stream().
                sorted(Comparator.comparing(TaskExec<?>::getEnd).reversed()).
                limit(max).
                collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected TaskExec<?> findLatest(final TaskType type, final Task<?> task, final String field) {
        Query query = entityManager().createQuery(
                "SELECT e FROM " + taskUtilsFactory.getInstance(type).getTaskExecEntity().getSimpleName() + " e "
                + "WHERE e.task=:task ORDER BY e." + field + " DESC");
        query.setParameter("task", task);
        query.setMaxResults(1);

        List<Object> result = query.getResultList();
        return result == null || result.isEmpty()
                ? null
                : (TaskExec<?>) result.get(0);
    }

    @Override
    public TaskExec<?> findLatestStarted(final TaskType type, final Task<?> task) {
        return findLatest(type, task, "start");
    }

    @Override
    public TaskExec<?> findLatestEnded(final TaskType type, final Task<?> task) {
        return findLatest(type, task, "end");
    }

    protected StringBuilder query(
            final StringBuilder select,
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        StringBuilder query = select.
                append(taskUtilsFactory.getInstance(task).getTaskExecEntity().getSimpleName()).
                append(" e WHERE e.task=:task ");
        if (before != null) {
            query.append("AND e.start <= :before ");
        }
        if (after != null) {
            query.append("AND e.start >= :after ");
        }
        return query;
    }

    @Override
    public int count(
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        StringBuilder queryString = query(new StringBuilder("SELECT COUNT(e) FROM "), task, before, after);

        Query query = entityManager().createQuery(queryString.toString());
        query.setParameter("task", task);
        if (before != null) {
            query.setParameter("before", before);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    protected String toOrderByStatement(final List<OrderByClause> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(AbstractTaskExec.class, field) != null) {
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

    @SuppressWarnings("unchecked")
    @Override
    public List<TaskExec<?>> findAll(
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderByClauses) {

        StringBuilder queryString = query(new StringBuilder("SELECT e FROM "), task, before, after).
                append(toOrderByStatement(orderByClauses));

        Query query = entityManager().createQuery(queryString.toString());
        query.setParameter("task", task);
        if (before != null) {
            query.setParameter("before", before);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        // page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        List<Object> result = query.getResultList();
        return result.stream().map(e -> (TaskExec<?>) e).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<T>> TaskExec<T> save(final TaskExec<T> execution) {
        return entityManager().merge(execution);
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<T>> void saveAndAdd(
            final TaskType taskType, final String taskKey, final TaskExec<T> execution) {

        T task = taskDAO.find(taskType, taskKey);
        task.add(execution);
        taskDAO.save(task);
    }

    @Override
    public <T extends Task<T>> void delete(final TaskType taskType, final String key) {
        TaskExec<T> execution = find(taskType, key);
        if (execution == null) {
            return;
        }

        delete(execution);
    }

    @Override
    public <T extends Task<T>> void delete(final TaskExec<T> execution) {
        if (execution.getTask() != null) {
            execution.getTask().getExecs().remove(execution);
        }

        entityManager().remove(execution);
    }
}
