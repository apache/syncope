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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAMacroTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAMacroTaskCommand;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTaskExec;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

public class JPATaskDAO implements TaskDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(TaskDAO.class);

    protected final RealmSearchDAO realmSearchDAO;

    protected final RemediationDAO remediationDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final SecurityProperties securityProperties;

    protected final EntityManager entityManager;

    public JPATaskDAO(
            final RealmSearchDAO realmSearchDAO,
            final RemediationDAO remediationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final SecurityProperties securityProperties,
            final EntityManager entityManager) {

        this.realmSearchDAO = realmSearchDAO;
        this.remediationDAO = remediationDAO;
        this.taskUtilsFactory = taskUtilsFactory;
        this.securityProperties = securityProperties;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsById(final String key) {
        return findById(key).isPresent();
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task<T>> Optional<T> findById(final TaskType type, final String key) {
        return Optional.ofNullable((T) entityManager.find(taskUtilsFactory.getInstance(type).getTaskEntity(), key));
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <T extends SchedTask> Optional<T> findByName(final TaskType type, final String name) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        TypedQuery<T> query = (TypedQuery<T>) entityManager.createQuery(
                "SELECT e FROM " + taskUtils.getTaskEntity().getSimpleName() + " e WHERE e.name = :name",
                taskUtils.getTaskEntity());
        query.setParameter("name", name);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            LOG.debug("No task found with name {}", name, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<? extends Task<?>> findById(final String key) {
        Optional<? extends Task<?>> task = findById(TaskType.SCHEDULED, key);
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

    @Override
    public List<SchedTask> findByDelegate(final Implementation delegate) {
        TypedQuery<SchedTask> query = entityManager.createQuery(
                "SELECT e FROM " + JPASchedTask.class.getSimpleName()
                + " e WHERE e.jobDelegate=:delegate", SchedTask.class);
        query.setParameter("delegate", delegate);

        return query.getResultList();
    }

    @Override
    public List<PullTask> findByReconFilterBuilder(final Implementation reconFilterBuilder) {
        TypedQuery<PullTask> query = entityManager.createQuery(
                "SELECT e FROM " + JPAPullTask.class.getSimpleName()
                + " e WHERE e.reconFilterBuilder=:reconFilterBuilder", PullTask.class);
        query.setParameter("reconFilterBuilder", reconFilterBuilder);

        return query.getResultList();
    }

    @Override
    public List<PullTask> findByInboundActions(final Implementation inboundActions) {
        TypedQuery<PullTask> query = entityManager.createQuery(
                "SELECT e FROM " + JPAPullTask.class.getSimpleName() + " e "
                + "WHERE :inboundActions MEMBER OF e.actions", PullTask.class);
        query.setParameter("inboundActions", inboundActions);

        return query.getResultList();
    }

    @Override
    public List<PushTask> findByPushActions(final Implementation pushActions) {
        TypedQuery<PushTask> query = entityManager.createQuery(
                "SELECT e FROM " + JPAPushTask.class.getSimpleName() + " e "
                + "WHERE :pushActions MEMBER OF e.actions", PushTask.class);
        query.setParameter("pushActions", pushActions);

        return query.getResultList();
    }

    @Override
    public List<MacroTask> findByRealm(final Realm realm) {
        TypedQuery<MacroTask> query = entityManager.createQuery(
                "SELECT e FROM " + JPAMacroTask.class.getSimpleName() + " e "
                + "WHERE e.realm=:realm", MacroTask.class);
        query.setParameter("realm", realm);

        return query.getResultList();
    }

    @Override
    public List<MacroTask> findByCommand(final Implementation command) {
        TypedQuery<MacroTask> query = entityManager.createQuery(
                "SELECT e.macroTask FROM " + JPAMacroTaskCommand.class.getSimpleName() + " e "
                + "WHERE e.command=:command", MacroTask.class);
        query.setParameter("command", command);

        return query.getResultList();
    }

    protected final <T extends Task<T>> StringBuilder buildFindAllQuery(final TaskType type) {
        StringBuilder builder = new StringBuilder("SELECT t FROM ").
                append(taskUtilsFactory.getInstance(type).getTaskEntity().getSimpleName()).
                append(" t WHERE ");
        if (type == TaskType.SCHEDULED) {
            builder.append("t.id NOT IN (SELECT t.id FROM ").
                    append(JPAPushTask.class.getSimpleName()).append(" t) ").
                    append("AND ").
                    append("t.id NOT IN (SELECT t.id FROM ").
                    append(JPAPullTask.class.getSimpleName()).append(" t)");
        } else {
            builder.append("1=1");
        }

        return builder.append(' ');
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task<T>> List<T> findToExec(final TaskType type) {
        StringBuilder queryString = buildFindAllQuery(type).append("AND ");

        if (type == TaskType.NOTIFICATION) {
            queryString.append("t.executed = false ");
        } else {
            queryString.append("t.executions IS EMPTY ");
        }
        queryString.append("ORDER BY t.id DESC");

        Query query = entityManager.createQuery(queryString.toString());
        return query.getResultList();
    }

    @Override
    public List<? extends Task<?>> findAll() {
        throw new UnsupportedOperationException();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends Task<T>> List<T> findAll(final TaskType type) {
        return findAll(type, null, null, null, null, Pageable.unpaged());
    }

    protected int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected StringBuilder buildFindAllQuery(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final boolean orderByTaskExecInfo,
            final List<Object> parameters) {

        if (resource != null
                && type != TaskType.PROPAGATION
                && type != TaskType.LIVE_SYNC
                && type != TaskType.PUSH
                && type != TaskType.PULL) {

            throw new IllegalArgumentException(type + " is not related to " + ExternalResource.class.getSimpleName());
        }

        if ((anyTypeKind != null || entityKey != null)
                && type != TaskType.PROPAGATION && type != TaskType.NOTIFICATION) {

            throw new IllegalArgumentException(type + " is not related to users, groups or any objects");
        }

        if (notification != null && type != TaskType.NOTIFICATION) {
            throw new IllegalArgumentException(type + " is not related to notifications");
        }

        String taskTable = taskUtilsFactory.getInstance(type).getTaskStorage();
        StringBuilder queryString = new StringBuilder("SELECT ").append(taskTable).append(".*");

        if (orderByTaskExecInfo) {
            String taskExecTable = taskUtilsFactory.getInstance(type).getTaskExecStorage();
            queryString.append(',').append(taskExecTable).append(".startDate AS startDate").
                    append(',').append(taskExecTable).append(".endDate AS endDate").
                    append(',').append(taskExecTable).append(".status AS status").
                    append(" FROM ").append(taskTable).
                    append(',').append(taskExecTable).append(',').append("(SELECT ").
                    append(taskExecTable).append(".task_id, ").
                    append("MAX(").append(taskExecTable).append(".startDate) AS startDate").
                    append(" FROM ").append(taskExecTable).
                    append(" GROUP BY ").append(taskExecTable).append(".task_id) GRP").
                    append(" WHERE ").
                    append(taskTable).append(".id=").append(taskExecTable).append(".task_id").
                    append(" AND ").append(taskTable).append(".id=").append("GRP.task_id").
                    append(" AND ").
                    append(taskExecTable).append(".startDate=").append("GRP.startDate");
        } else {
            queryString.append(", null AS startDate, null AS endDate, null AS status FROM ").append(taskTable).
                    append(" WHERE 1=1");
        }

        queryString.append(' ');

        if (resource != null) {
            queryString.append(" AND ").
                    append(taskTable).append(".resource_id=?").append(setParameter(parameters, resource.getKey()));
        }
        if (notification != null) {
            queryString.append(" AND ").
                    append(taskTable).
                    append(".notification_id=?").append(setParameter(parameters, notification.getKey()));
        }
        if (anyTypeKind != null) {
            queryString.append(" AND ").
                    append(taskTable).append(".anyTypeKind=?").append(setParameter(parameters, anyTypeKind.name()));
        }
        if (entityKey != null) {
            queryString.append(" AND ").
                    append(taskTable).append(".entityKey=?").append(setParameter(parameters, entityKey));
        }
        if (type == TaskType.MACRO
                && !AuthContextUtils.getUsername().equals(securityProperties.getAdminUser())) {

            String realmKeysArg = AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.TASK_LIST).stream().
                    map(realmSearchDAO::findByFullPath).
                    filter(Optional::isPresent).
                    flatMap(r -> realmSearchDAO.findDescendants(r.get().getFullPath(), null, Pageable.unpaged()).
                    stream()).
                    map(Realm::getKey).
                    distinct().
                    map(realmKey -> "?" + setParameter(parameters, realmKey)).
                    collect(Collectors.joining(","));
            queryString.append(" AND ").
                    append(taskTable).append(".realm_id IN (").append(realmKeysArg).append(")");
        }

        return queryString;
    }

    protected String toOrderByStatement(
            final Class<? extends Task<?>> beanClass,
            final Stream<Sort.Order> orderByClauses) {

        StringBuilder statement = new StringBuilder();

        statement.append(" ORDER BY ");

        StringBuilder subStatement = new StringBuilder();
        orderByClauses.forEach(clause -> {
            String field = clause.getProperty().trim();
            switch (field) {
                case "latestExecStatus":
                    field = "status";
                    break;

                case "start":
                    field = "startDate";
                    break;

                case "end":
                    field = "endDate";
                    break;

                default:
                    Field beanField = ReflectionUtils.findField(beanClass, field);
                    if (beanField != null
                            && (beanField.getAnnotation(ManyToOne.class) != null
                            || beanField.getAnnotation(OneToMany.class) != null
                            || beanField.getAnnotation(OneToOne.class) != null)) {

                        field += "_id";
                    }
            }

            subStatement.append(field).append(' ').append(clause.getDirection().name()).append(',');
        });

        if (subStatement.length() == 0) {
            statement.append("id DESC");
        } else {
            subStatement.deleteCharAt(subStatement.length() - 1);
            statement.append(subStatement);
        }

        return statement.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task<T>> List<T> findAll(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final Pageable pageable) {

        List<Object> parameters = new ArrayList<>();

        boolean orderByTaskExecInfo = pageable.getSort().stream().
                anyMatch(clause -> clause.getProperty().equals("start")
                || clause.getProperty().equals("end")
                || clause.getProperty().equals("latestExecStatus")
                || clause.getProperty().equals("status"));

        StringBuilder queryString = buildFindAllQuery(
                type,
                resource,
                notification,
                anyTypeKind,
                entityKey,
                orderByTaskExecInfo,
                parameters);

        if (orderByTaskExecInfo) {
            // UNION with tasks without executions...
            queryString.insert(0, "SELECT T.id FROM ((").append(") UNION ALL (").
                    append(buildFindAllQuery(
                            type,
                            resource,
                            notification,
                            anyTypeKind,
                            entityKey,
                            false,
                            parameters)).
                    append(" AND id NOT IN ").
                    append("(SELECT task_id AS id FROM ").
                    append(taskUtilsFactory.getInstance(type).getTaskExecStorage()).
                    append(')').
                    append(")) T");
        } else {
            queryString.insert(0, "SELECT T.id FROM (").append(") T");
        }

        queryString.append(toOrderByStatement(
                taskUtilsFactory.getInstance(type).getTaskEntity(), pageable.getSort().stream()));

        Query query = entityManager.createNativeQuery(queryString.toString());

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        List<T> result = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Object> raw = query.getResultList();
        raw.forEach(key -> findById(type, key.toString()).ifPresentOrElse(
                task -> result.add((T) task),
                () -> LOG.error("Could not find task with key {}, even if returned by native query", key)));

        return result;
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString =
                buildFindAllQuery(type, resource, notification, anyTypeKind, entityKey, false, parameters);

        String table = taskUtilsFactory.getInstance(type).getTaskStorage();
        Query query = entityManager.createNativeQuery(StringUtils.replaceOnce(
                queryString.toString(),
                "SELECT " + table + ".*, null AS startDate, null AS endDate, null AS status",
                "SELECT COUNT(" + table + ".id)"));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<?>> T save(final T task) {
        switch (task) {
            case JPANotificationTask jpaNotificationTask ->
                jpaNotificationTask.list2json();
            case JPAPushTask jpaPushTask ->
                jpaPushTask.map2json();
            default -> {
            }
        }
        return entityManager.merge(task);
    }

    @Override
    public void delete(final TaskType type, final String key) {
        findById(type, key).ifPresent(this::delete);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public void delete(final Task<?> task) {
        if (task instanceof PullTask pullTask) {
            remediationDAO.findByPullTask(pullTask).forEach(remediation -> remediation.setPullTask(null));
        }

        entityManager.remove(task);
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        findAll(type, resource, null, null, null, Pageable.unpaged()).
                stream().map(Task<?>::getKey).forEach(key -> delete(type, key));
    }

    @Override
    public List<PropagationTaskTO> purgePropagations(
            final OffsetDateTime since,
            final List<ExecStatus> statuses,
            final List<String> resources) {

        StringBuilder queryString = new StringBuilder("SELECT t.task_id "
                + "FROM " + JPAPropagationTaskExec.TABLE + " t "
                + "INNER JOIN " + JPAPropagationTask.TABLE + " z "
                + "ON t.task_id=z.id "
                + "WHERE t.enddate=(SELECT MAX(e.enddate) FROM " + JPAPropagationTaskExec.TABLE + " e "
                + "WHERE e.task_id=t.task_id) ");

        List<Object> queryParameters = new ArrayList<>();
        if (since != null) {
            queryParameters.add(since);
            queryString.append("AND t.enddate <= ?").append(queryParameters.size()).append(' ');
        }
        if (!CollectionUtils.isEmpty(statuses)) {
            queryString.append("AND (").
                    append(statuses.stream().map(status -> {
                        queryParameters.add(status.name());
                        return "t.status = ?" + queryParameters.size();
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }
        if (!CollectionUtils.isEmpty(resources)) {
            queryString.append("AND (").
                    append(resources.stream().map(r -> {
                        queryParameters.add(r);
                        return "z.resource_id = ?" + queryParameters.size();
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }

        Query query = entityManager.createNativeQuery(queryString.toString());
        for (int i = 1; i <= queryParameters.size(); i++) {
            query.setParameter(i, queryParameters.get(i - 1));
        }

        @SuppressWarnings("unchecked")
        List<Object> raw = query.getResultList();

        List<PropagationTaskTO> purged = new ArrayList<>();
        raw.stream().map(Object::toString).distinct().forEach(key -> findById(TaskType.PROPAGATION, key).
                map(PropagationTask.class::cast).ifPresent(task -> {

            PropagationTaskTO taskTO = new PropagationTaskTO();

            taskTO.setOperation(task.getOperation());
            taskTO.setConnObjectKey(task.getConnObjectKey());
            taskTO.setOldConnObjectKey(task.getOldConnObjectKey());
            taskTO.setPropagationData(task.getSerializedPropagationData());
            taskTO.setResource(task.getResource().getKey());
            taskTO.setObjectClassName(task.getObjectClassName());
            taskTO.setAnyTypeKind(task.getAnyTypeKind());
            taskTO.setAnyType(task.getAnyType());
            taskTO.setEntityKey(task.getEntityKey());

            purged.add(taskTO);

            delete(task);
        }));

        return purged;
    }
}
