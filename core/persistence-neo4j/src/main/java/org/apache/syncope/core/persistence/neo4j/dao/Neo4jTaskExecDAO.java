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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.neo4j.entity.task.AbstractTaskExec;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class Neo4jTaskExecDAO extends AbstractDAO implements TaskExecDAO {

    protected final TaskDAO taskDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final NodeValidator nodeValidator;

    public Neo4jTaskExecDAO(
            final TaskDAO taskDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.taskDAO = taskDAO;
        this.taskUtilsFactory = taskUtilsFactory;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public boolean existsById(final String key) {
        return findById(key).isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task<T>> Optional<TaskExec<T>> findById(final TaskType type, final String key) {
        return neo4jTemplate.findById(key, (Class<TaskExec<T>>) taskUtilsFactory.getInstance(type).getTaskExecEntity());
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
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        return toList(neo4jClient.query(
                "MATCH (n:" + taskUtils.getTaskExecStorage() + ")-[]-(p:" + taskUtils.getTaskStorage() + " {id: $id}) "
                + "WHERE n.endDate IS NOT NULL "
                + "RETURN n.id ORDER BY n.endDate DESC LIMIT " + max).fetch().all(),
                "n.id",
                (Class<AbstractTaskExec<T>>) taskUtils.getTaskExecEntity(),
                null);
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
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        return neo4jClient.query(
                "MATCH (n:" + taskUtils.getTaskExecStorage() + ")-[]-(p:" + taskUtils.getTaskStorage() + " {id: $id}) "
                + "RETURN n.id ORDER BY n." + field + " DESC LIMIT 1").
                bindAll(Map.of("id", task.getKey())).fetch().one().
                flatMap(toOptional("n.id", (Class<AbstractTaskExec<?>>) taskUtils.getTaskExecEntity(), null));
    }

    @Override
    public Optional<? extends TaskExec<?>> findLatestStarted(final TaskType type, final Task<?> task) {
        return findLatest(type, task, "startDate");
    }

    @Override
    public Optional<? extends TaskExec<?>> findLatestEnded(final TaskType type, final Task<?> task) {
        return findLatest(type, task, "endDate");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    protected StringBuilder query(
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Map<String, Object> parameters) {

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        parameters.put("id", task.getKey());

        StringBuilder query = new StringBuilder(
                "MATCH (n:").append(taskUtils.getTaskExecStorage()).append(")-").
                append("[:").append(Neo4jTaskDAO.execRelationship(taskUtils.getType())).append("]-").
                append("(p:").append(taskUtils.getTaskStorage()).append(" {id: $id}) ").
                append("WHERE 1=1 ");

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
            final Task<?> task,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = query(task, before, after, parameters).append(" RETURN COUNT(n)");
        return neo4jTemplate.count(queryString.toString(), parameters);
    }

    protected String toOrderByStatement(final Stream<Sort.Order> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getProperty().trim();
            if (ReflectionUtils.findField(AbstractTaskExec.class, field) != null) {
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

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = query(task, before, after, parameters).
                append(" RETURN n.id ").append(toOrderByStatement(pageable.getSort().stream()));
        if (pageable.isPaged()) {
            queryString.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(
                queryString.toString()).bindAll(parameters).fetch().all(),
                "n.id",
                (Class<AbstractTaskExec<?>>) taskUtilsFactory.getInstance(task).getTaskExecEntity(),
                null);
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <S extends TaskExec<?>> S save(final S execution) {
        return neo4jTemplate.save(nodeValidator.validate(execution));
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

        neo4jTemplate.deleteById(execution.getKey(), execution.getClass());
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
