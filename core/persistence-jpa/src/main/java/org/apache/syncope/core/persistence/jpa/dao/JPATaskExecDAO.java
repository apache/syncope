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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.task.AbstractTaskExec;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

public class JPATaskExecDAO implements TaskExecDAO {

    protected final TaskDAO taskDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final EntityManager entityManager;

    public JPATaskExecDAO(
            final TaskDAO taskDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final EntityManager entityManager) {

        this.taskDAO = taskDAO;
        this.taskUtilsFactory = taskUtilsFactory;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsById(final String key) {
        return findById(key).isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task<T>> Optional<TaskExec<T>> findById(final TaskType type, final String key) {
        return Optional.ofNullable(
                (TaskExec<T>) entityManager.find(taskUtilsFactory.getInstance(type).getTaskExecEntity(), key));
    }

    @Override
    public Optional<? extends TaskExec<?>> findById(final String key) {
        Optional<? extends TaskExec<?>> task = findById(TaskType.SCHEDULED, key);
        if (task.isEmpty()) {
            task = findById(TaskType.PULL, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.PUSH, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.MACRO, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.PROPAGATION, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.NOTIFICATION, key);
        }

        return task;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Task<T>> List<TaskExec<T>> findRecent(final TaskType type, final int max) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + taskUtilsFactory.getInstance(type).getTaskExecEntity().getSimpleName() + " e "
                + "WHERE e.end IS NOT NULL ORDER BY e.end DESC");
        query.setMaxResults(max);

        List<Object> result = query.getResultList();
        return result.stream().map(e -> (TaskExec<T>) e).toList();
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
                toList();
    }

    @SuppressWarnings("unchecked")
    protected Optional<? extends TaskExec<?>> findLatest(final TaskType type, final Task<?> task, final String field) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + taskUtilsFactory.getInstance(type).getTaskExecEntity().getSimpleName() + " e "
                + "WHERE e.task.id=:task ORDER BY e." + field + " DESC");
        query.setParameter("task", task.getKey());
        query.setMaxResults(1);

        List<Object> result = query.getResultList();
        return CollectionUtils.isEmpty(result)
                ? Optional.empty()
                : Optional.of((TaskExec<?>) result.getFirst());
    }

    @Override
    public Optional<? extends TaskExec<?>> findLatestStarted(final TaskType type, final Task<?> task) {
        return findLatest(type, task, "start");
    }

    @Override
    public Optional<? extends TaskExec<?>> findLatestEnded(final TaskType type, final Task<?> task) {
        return findLatest(type, task, "end");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
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
    public long count(
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        StringBuilder queryString = query(new StringBuilder("SELECT COUNT(e) FROM "), task, before, after);

        Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("task", task);
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

    @Override
    public List<? extends TaskExec<?>> findAll() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TaskExec<?>> findAll(
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        StringBuilder queryString = query(new StringBuilder("SELECT e FROM "), task, before, after).
                append(toOrderByStatement(pageable.getSort().stream()));

        Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("task", task);
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

        List<Object> result = query.getResultList();
        return result.stream().map(e -> (TaskExec<?>) e).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <S extends TaskExec<?>> S save(final S execution) {
        return entityManager.merge(execution);
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<T>> void saveAndAdd(
            final TaskType taskType, final String taskKey, final TaskExec<T> execution) {

        Optional<T> task = taskDAO.findById(taskType, taskKey);
        if (task.isPresent()) {
            task.get().add(execution);
            taskDAO.save(task.get());
        }
    }

    @Override
    public <T extends Task<T>> void delete(final TaskType taskType, final String key) {
        findById(taskType, key).ifPresent(this::delete);
    }

    @Override
    public void delete(final TaskExec<?> execution) {
        Optional.ofNullable(execution.getTask()).ifPresent(task -> task.getExecs().remove(execution));

        entityManager.remove(execution);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
