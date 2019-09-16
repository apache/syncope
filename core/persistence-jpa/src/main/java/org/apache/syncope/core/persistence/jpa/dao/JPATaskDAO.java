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
import java.util.ArrayList;
import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.AbstractTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

@Repository
public class JPATaskDAO extends AbstractDAO<Task> implements TaskDAO {

    @Autowired
    private RemediationDAO remediationDAO;

    @Override
    public Class<? extends Task> getEntityReference(final TaskType type) {
        Class<? extends Task> result = null;

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

            case SCHEDULED:
                result = JPASchedTask.class;
                break;

            case PULL:
                result = JPAPullTask.class;
                break;

            default:
        }

        return result;
    }

    private static String getEntityTableName(final TaskType type) {
        String result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTask.class.getAnnotation(DiscriminatorValue.class).value();
                break;

            case PROPAGATION:
                result = JPAPropagationTask.class.getAnnotation(DiscriminatorValue.class).value();
                break;

            case PUSH:
                result = JPAPushTask.class.getAnnotation(DiscriminatorValue.class).value();
                break;

            case SCHEDULED:
                result = JPASchedTask.class.getAnnotation(DiscriminatorValue.class).value();
                break;

            case PULL:
                result = JPAPullTask.class.getAnnotation(DiscriminatorValue.class).value();
                break;

            default:
        }

        return result;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task> T find(final String key) {
        return (T) entityManager().find(AbstractTask.class, key);
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

    private <T extends Task> StringBuilder buildFindAllQueryJPA(final TaskType type) {
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
    public <T extends Task> List<T> findToExec(final TaskType type) {
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
    public <T extends Task> List<T> findAll(final TaskType type) {
        return findAll(type, null, null, null, null, -1, -1, List.of());
    }

    private static StringBuilder buildFindAllQuery(
        final TaskType type,
        final ExternalResource resource,
        final Notification notification,
        final AnyTypeKind anyTypeKind,
        final String entityKey,
        final boolean orderByTaskExecInfo,
        final List<Object> queryParameters) {

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

        StringBuilder queryString = new StringBuilder("SELECT ").append(AbstractTask.TABLE).append(".*");

        if (orderByTaskExecInfo) {
            queryString.append(',').append(JPATaskExec.TABLE).append(".startDate AS startDate").
                    append(',').append(JPATaskExec.TABLE).append(".endDate AS endDate").
                    append(',').append(JPATaskExec.TABLE).append(".status AS status").
                    append(" FROM ").append(AbstractTask.TABLE).
                    append(',').append(JPATaskExec.TABLE).append(',').append("(SELECT ").
                    append(JPATaskExec.TABLE).append(".task_id, ").
                    append("MAX(").append(JPATaskExec.TABLE).append(".startDate) AS startDate").
                    append(" FROM ").append(JPATaskExec.TABLE).
                    append(" GROUP BY ").append(JPATaskExec.TABLE).append(".task_id) GRP").
                    append(" WHERE ").
                    append(AbstractTask.TABLE).append(".id=").append(JPATaskExec.TABLE).append(".task_id").
                    append(" AND ").append(AbstractTask.TABLE).append(".id=").append("GRP.task_id").
                    append(" AND ").append(JPATaskExec.TABLE).append(".startDate=").append("GRP.startDate").
                    append(" AND ").append(AbstractTask.TABLE).append(".DTYPE = ?1");
        } else {
            queryString.append(", null AS startDate, null AS endDate, null AS status FROM ").append(AbstractTask.TABLE).
                    append(" WHERE ").append(AbstractTask.TABLE).append(".DTYPE = ?1");
        }

        queryParameters.add(getEntityTableName(type));
        if (type == TaskType.SCHEDULED) {
            queryString.append(" AND ").
                    append(AbstractTask.TABLE).
                    append(".id NOT IN (SELECT ").append(AbstractTask.TABLE).append(".id FROM ").
                    append(AbstractTask.TABLE).append(" WHERE ").
                    append(AbstractTask.TABLE).append(".DTYPE = ?2)").
                    append(" AND ").
                    append(AbstractTask.TABLE).
                    append(".id NOT IN (SELECT id FROM ").
                    append(AbstractTask.TABLE).append(" WHERE ").
                    append(AbstractTask.TABLE).append(".DTYPE = ?3)");

            queryParameters.add(JPAPushTask.class.getAnnotation(DiscriminatorValue.class).value());
            queryParameters.add(JPAPullTask.class.getAnnotation(DiscriminatorValue.class).value());
        }
        queryString.append(' ');

        if (resource != null) {
            queryParameters.add(resource.getKey());

            queryString.append(" AND ").
                    append(AbstractTask.TABLE).
                    append(".resource_id=?").append(queryParameters.size());
        }
        if (notification != null) {
            queryParameters.add(notification.getKey());

            queryString.append(" AND ").
                    append(AbstractTask.TABLE).
                    append(".notification_id=?").append(queryParameters.size());
        }
        if (anyTypeKind != null && entityKey != null) {
            queryParameters.add(anyTypeKind.name());
            queryParameters.add(entityKey);

            queryString.append(" AND ").
                    append(AbstractTask.TABLE).
                    append(".anyTypeKind=?").append(queryParameters.size() - 1).
                    append(" AND ").
                    append(AbstractTask.TABLE).
                    append(".entityKey=?").append(queryParameters.size());
        }

        return queryString;
    }

    private static String toOrderByStatement(final Class<? extends Task> beanClass,
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
                            || beanField.getAnnotation(OneToMany.class) != null)) {
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
    @SuppressWarnings("unchecked")
    public <T extends Task> List<T> findAll(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderByClauses) {

        List<Object> queryParameters = new ArrayList<>();

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
                queryParameters);

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
                            queryParameters)).
                    append(" AND id NOT IN ").
                    append("(SELECT task_id AS id FROM ").append(JPATaskExec.TABLE).append(')').
                    append(")) T");
        } else {
            queryString.insert(0, "SELECT T.id FROM (").append(") T");
        }

        queryString.append(toOrderByStatement(getEntityReference(type), orderByClauses));

        Query query = entityManager().createNativeQuery(queryString.toString());

        for (int i = 1; i <= queryParameters.size(); i++) {
            query.setParameter(i, queryParameters.get(i - 1));
        }

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return buildResult(query.getResultList());
    }

    @Override
    public int count(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

        List<Object> queryParameters = new ArrayList<>();

        StringBuilder queryString =
                buildFindAllQuery(type, resource, notification, anyTypeKind, entityKey, false, queryParameters);

        Query query = entityManager().createNativeQuery(StringUtils.replaceOnce(
                queryString.toString(),
                "SELECT " + AbstractTask.TABLE + ".*, null AS startDate, null AS endDate, null AS status",
                "SELECT COUNT(" + AbstractTask.TABLE + ".id)"));

        for (int i = 1; i <= queryParameters.size(); i++) {
            query.setParameter(i, queryParameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task> T save(final T task) {
        return entityManager().merge(task);
    }

    @Override
    public void delete(final String id) {
        Task task = find(id);
        if (task == null) {
            return;
        }

        delete(task);
    }

    @Override
    public void delete(final Task task) {
        if (task instanceof PullTask) {
            remediationDAO.findByPullTask((PullTask) task).forEach(remediation -> remediation.setPullTask(null));
        }

        entityManager().remove(task);
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        findAll(type, resource, null, null, null, -1, -1, List.of()).
                stream().map(Entity::getKey).forEach(this::delete);
    }

    private <T extends Task> List<T> buildResult(final List<Object> raw) {
        List<T> result = new ArrayList<>();

        for (Object anyKey : raw) {
            String actualKey = anyKey instanceof Object[]
                    ? (String) ((Object[]) anyKey)[0]
                    : ((String) anyKey);

            @SuppressWarnings("unchecked")
            T task = find(actualKey);
            if (task == null) {
                LOG.error("Could not find task with id {}, even if returned by native query", actualKey);
            } else if (!result.contains(task)) {
                result.add(task);
            }
        }

        return result;
    }
}
