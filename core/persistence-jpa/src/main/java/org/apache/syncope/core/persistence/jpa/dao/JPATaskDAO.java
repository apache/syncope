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

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.CommandTask;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.task.JPACommandTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTaskExec;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

public class JPATaskDAO extends AbstractDAO<Task<?>> implements TaskDAO {

    public static TaskType getTaskType(final Task<?> task) {
        if (task instanceof NotificationTask) {
            return TaskType.NOTIFICATION;
        }

        if (task instanceof PropagationTask) {
            return TaskType.PROPAGATION;
        }

        if (task instanceof PushTask) {
            return TaskType.PUSH;
        }

        if (task instanceof PullTask) {
            return TaskType.PULL;
        }

        if (task instanceof CommandTask) {
            return TaskType.COMMAND;
        }

        if (task instanceof SchedTask) {
            return TaskType.SCHEDULED;
        }

        return null;
    }

    public static String getEntityTableName(final TaskType type) {
        String result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTask.TABLE;
                break;

            case PROPAGATION:
                result = JPAPropagationTask.TABLE;
                break;

            case PUSH:
                result = JPAPushTask.TABLE;
                break;

            case PULL:
                result = JPAPullTask.TABLE;
                break;

            case COMMAND:
                result = JPACommandTask.TABLE;
                break;

            case SCHEDULED:
                result = JPASchedTask.TABLE;
                break;

            default:
        }

        return result;
    }

    public static Class<? extends Task<?>> getEntityReference(final TaskType type) {
        Class<? extends Task<?>> result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTask.class;
                break;

            case PROPAGATION:
                result = JPAPropagationTask.class;
                break;

            case PUSH:
                result = JPAPushTask.class;
                break;

            case PULL:
                result = JPAPullTask.class;
                break;

            case COMMAND:
                result = JPACommandTask.class;
                break;

            case SCHEDULED:
                result = JPASchedTask.class;
                break;

            default:
        }

        return result;
    }

    protected final RealmDAO realmDAO;

    protected final RemediationDAO remediationDAO;

    protected final SecurityProperties securityProperties;

    public JPATaskDAO(
            final RealmDAO realmDAO,
            final RemediationDAO remediationDAO,
            final SecurityProperties securityProperties) {

        this.realmDAO = realmDAO;
        this.remediationDAO = remediationDAO;
        this.securityProperties = securityProperties;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(final TaskType type, final String key) {
        Query query = entityManager().createNativeQuery("SELECT id FROM " + getEntityTableName(type) + " WHERE id=?");
        query.setParameter(1, key);

        return !query.getResultList().isEmpty();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task<T>> T find(final TaskType type, final String key) {
        return (T) entityManager().find(getEntityReference(type), key);
    }

    @Override
    public Optional<Task<?>> find(final String key) {
        Task<?> task = find(TaskType.SCHEDULED, key);
        if (task == null) {
            task = find(TaskType.PULL, key);
        }
        if (task == null) {
            task = find(TaskType.PUSH, key);
        }
        if (task == null) {
            task = find(TaskType.COMMAND, key);
        }
        if (task == null) {
            task = find(TaskType.PROPAGATION, key);
        }
        if (task == null) {
            task = find(TaskType.NOTIFICATION, key);
        }

        return Optional.ofNullable(task);
    }

    @Override
    public List<SchedTask> findByDelegate(final Implementation delegate) {
        TypedQuery<SchedTask> query = entityManager().createQuery(
                "SELECT e FROM " + JPASchedTask.class.getSimpleName()
                + " e WHERE e.jobDelegate=:delegate", SchedTask.class);
        query.setParameter("delegate", delegate);

        return query.getResultList();
    }

    @Override
    public List<PullTask> findByReconFilterBuilder(final Implementation reconFilterBuilder) {
        TypedQuery<PullTask> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPullTask.class.getSimpleName()
                + " e WHERE e.reconFilterBuilder=:reconFilterBuilder", PullTask.class);
        query.setParameter("reconFilterBuilder", reconFilterBuilder);

        return query.getResultList();
    }

    @Override
    public List<PullTask> findByPullActions(final Implementation pullActions) {
        TypedQuery<PullTask> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPullTask.class.getSimpleName() + " e "
                + "WHERE :pullActions MEMBER OF e.actions", PullTask.class);
        query.setParameter("pullActions", pullActions);

        return query.getResultList();
    }

    @Override
    public List<PushTask> findByPushActions(final Implementation pushActions) {
        TypedQuery<PushTask> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPushTask.class.getSimpleName() + " e "
                + "WHERE :pushActions MEMBER OF e.actions", PushTask.class);
        query.setParameter("pushActions", pushActions);

        return query.getResultList();
    }

    @Override
    public List<CommandTask> findByRealm(final Realm realm) {
        TypedQuery<CommandTask> query = entityManager().createQuery(
                "SELECT e FROM " + JPACommandTask.class.getSimpleName() + " e "
                + "WHERE e.realm=:realm", CommandTask.class);
        query.setParameter("realm", realm);

        return query.getResultList();
    }

    protected final <T extends Task<T>> StringBuilder buildFindAllQueryJPA(final TaskType type) {
        StringBuilder builder = new StringBuilder("SELECT t FROM ").
                append(getEntityReference(type).getSimpleName()).
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
        StringBuilder queryString = buildFindAllQueryJPA(type).append("AND ");

        if (type == TaskType.NOTIFICATION) {
            queryString.append("t.executed = false ");
        } else {
            queryString.append("t.executions IS EMPTY ");
        }
        queryString.append("ORDER BY t.id DESC");

        Query query = entityManager().createQuery(queryString.toString());
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends Task<T>> List<T> findAll(final TaskType type) {
        return findAll(type, null, null, null, null, -1, -1, List.of());
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
                && type != TaskType.PROPAGATION && type != TaskType.PUSH && type != TaskType.PULL) {

            throw new IllegalArgumentException(type + " is not related to " + ExternalResource.class.getSimpleName());
        }

        if ((anyTypeKind != null || entityKey != null)
                && type != TaskType.PROPAGATION && type != TaskType.NOTIFICATION) {

            throw new IllegalArgumentException(type + " is not related to users, groups or any objects");
        }

        if (notification != null && type != TaskType.NOTIFICATION) {
            throw new IllegalArgumentException(type + " is not related to notifications");
        }

        String table = getEntityTableName(type);
        StringBuilder queryString = new StringBuilder("SELECT ").append(table).append(".*");

        if (orderByTaskExecInfo) {
            queryString.append(',').append(JPATaskExecDAO.getEntityTableName(type)).append(".startDate AS startDate").
                    append(',').append(JPATaskExecDAO.getEntityTableName(type)).append(".endDate AS endDate").
                    append(',').append(JPATaskExecDAO.getEntityTableName(type)).append(".status AS status").
                    append(" FROM ").append(table).
                    append(',').append(JPATaskExecDAO.getEntityTableName(type)).append(',').append("(SELECT ").
                    append(JPATaskExecDAO.getEntityTableName(type)).append(".task_id, ").
                    append("MAX(").append(JPATaskExecDAO.getEntityTableName(type)).append(".startDate) AS startDate").
                    append(" FROM ").append(JPATaskExecDAO.getEntityTableName(type)).
                    append(" GROUP BY ").append(JPATaskExecDAO.getEntityTableName(type)).append(".task_id) GRP").
                    append(" WHERE ").
                    append(table).append(".id=").append(JPATaskExecDAO.getEntityTableName(type)).append(".task_id").
                    append(" AND ").append(table).append(".id=").append("GRP.task_id").
                    append(" AND ").
                    append(JPATaskExecDAO.getEntityTableName(type)).append(".startDate=").append("GRP.startDate");
        } else {
            queryString.append(", null AS startDate, null AS endDate, null AS status FROM ").append(table).
                    append(" WHERE 1=1");
        }

        queryString.append(' ');

        if (resource != null) {
            queryString.append(" AND ").
                    append(table).append(".resource_id=?").append(setParameter(parameters, resource.getKey()));
        }
        if (notification != null) {
            queryString.append(" AND ").
                    append(table).
                    append(".notification_id=?").append(setParameter(parameters, notification.getKey()));
        }
        if (anyTypeKind != null && entityKey != null) {
            queryString.append(" AND ").
                    append(table).append(".anyTypeKind=?").append(setParameter(parameters, anyTypeKind.name())).
                    append(" AND ").
                    append(table).append(".entityKey=?").append(setParameter(parameters, entityKey));
        }
        if (type == TaskType.COMMAND
                && !AuthContextUtils.getUsername().equals(securityProperties.getAdminUser())) {

            String realmKeysArg = AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.TASK_LIST).stream().
                    map(realmDAO::findByFullPath).
                    filter(Objects::nonNull).
                    flatMap(r -> realmDAO.findDescendants(r).stream()).
                    map(Realm::getKey).
                    distinct().
                    map(realmKey -> "?" + setParameter(parameters, realmKey)).
                    collect(Collectors.joining(","));
            queryString.append(" AND ").
                    append(table).append(".realm_id IN (").append(realmKeysArg).append(")");
        }

        return queryString;
    }

    protected String toOrderByStatement(
            final Class<? extends Task<?>> beanClass,
            final List<OrderByClause> orderByClauses) {

        StringBuilder statement = new StringBuilder();

        statement.append(" ORDER BY ");

        StringBuilder subStatement = new StringBuilder();
        orderByClauses.forEach(clause -> {
            String field = clause.getField().trim();
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

    @Override
    public <T extends Task<T>> List<T> findAll(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderByClauses) {

        List<Object> parameters = new ArrayList<>();

        boolean orderByTaskExecInfo = orderByClauses.stream().
                anyMatch(clause -> clause.getField().equals("start")
                || clause.getField().equals("end")
                || clause.getField().equals("latestExecStatus")
                || clause.getField().equals("status"));

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
                    append("(SELECT task_id AS id FROM ").append(JPATaskExecDAO.getEntityTableName(type)).append(')').
                    append(")) T");
        } else {
            queryString.insert(0, "SELECT T.id FROM (").append(") T");
        }

        queryString.append(toOrderByStatement(getEntityReference(type), orderByClauses));

        Query query = entityManager().createNativeQuery(queryString.toString());

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        List<T> result = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Object> raw = query.getResultList();
        raw.stream().map(key -> key instanceof Object[]
                ? (String) ((Object[]) key)[0]
                : ((String) key)).forEach(key -> {

            T task = find(type, key);
            if (task == null) {
                LOG.error("Could not find task with key {}, even if returned by native query", key);
            } else if (!result.contains(task)) {
                result.add(task);
            }
        });

        return result;
    }

    @Override
    public int count(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString =
                buildFindAllQuery(type, resource, notification, anyTypeKind, entityKey, false, parameters);

        Query query = entityManager().createNativeQuery(StringUtils.replaceOnce(
                queryString.toString(),
                "SELECT " + getEntityTableName(type) + ".*, null AS startDate, null AS endDate, null AS status",
                "SELECT COUNT(" + getEntityTableName(type) + ".id)"));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<T>> T save(final T task) {
        if (task instanceof JPANotificationTask) {
            ((JPANotificationTask) task).list2json();
        }
        return entityManager().merge(task);
    }

    @Override
    public void delete(final TaskType type, final String id) {
        Task<?> task = find(type, id);
        if (task == null) {
            return;
        }

        delete(task);
    }

    @Override
    public void delete(final Task<?> task) {
        if (task instanceof PullTask) {
            remediationDAO.findByPullTask((PullTask) task).forEach(remediation -> remediation.setPullTask(null));
        }

        entityManager().remove(task);
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        findAll(type, resource, null, null, null, -1, -1, List.of()).
                stream().map(Task<?>::getKey).forEach(key -> delete(type, key));
    }

    @Override
    public List<PropagationTaskTO> purgePropagations(
            final OffsetDateTime since,
            final List<ExecStatus> statuses,
            final List<ExternalResource> externalResources) {

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
        if (!CollectionUtils.isEmpty(externalResources)) {
            queryString.append("AND (").
                    append(externalResources.stream().map(externalResource -> {
                        queryParameters.add(externalResource.getKey());
                        return "z.resource_id = ?" + queryParameters.size();
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }

        Query query = entityManager().createNativeQuery(queryString.toString());
        for (int i = 1; i <= queryParameters.size(); i++) {
            query.setParameter(i, queryParameters.get(i - 1));
        }

        @SuppressWarnings("unchecked")
        List<Object> raw = query.getResultList();

        List<PropagationTaskTO> purged = new ArrayList<>();
        raw.stream().map(Object::toString).distinct().forEach(key -> {
            PropagationTask task = find(TaskType.PROPAGATION, key);
            if (task != null) {
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
            }
        });

        return purged;
    }
}
