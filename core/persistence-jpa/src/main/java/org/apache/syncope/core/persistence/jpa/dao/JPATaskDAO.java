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
import java.util.Collections;
import java.util.List;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
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
import org.apache.syncope.core.provisioning.api.event.TaskCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.TaskDeletedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

@Repository
public class JPATaskDAO extends AbstractDAO<Task> implements TaskDAO {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private RemediationDAO remediationDAO;

    private <T extends Task> Class<? extends AbstractTask> getEntityReference(final Class<T> reference) {
        return PropagationTask.class.isAssignableFrom(reference)
                ? JPAPropagationTask.class
                : NotificationTask.class.isAssignableFrom(reference)
                ? JPANotificationTask.class
                : PullTask.class.isAssignableFrom(reference)
                ? JPAPullTask.class
                : PushTask.class.isAssignableFrom(reference)
                ? JPAPushTask.class
                : SchedTask.class.isAssignableFrom(reference)
                ? JPASchedTask.class
                : null;
    }

    private <T extends Task> String getEntityTableName(final Class<T> reference) {
        return PropagationTask.class.isAssignableFrom(reference)
                ? JPAPropagationTask.TABLE
                : NotificationTask.class.isAssignableFrom(reference)
                ? JPANotificationTask.TABLE
                : PullTask.class.isAssignableFrom(reference)
                ? JPAPullTask.TABLE
                : PushTask.class.isAssignableFrom(reference)
                ? JPAPushTask.TABLE
                : SchedTask.class.isAssignableFrom(reference)
                ? JPASchedTask.TABLE
                : null;
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

    private <T extends Task> StringBuilder buildFindAllQueryJPA(final Class<T> reference) {
        StringBuilder builder = new StringBuilder("SELECT t FROM ").
                append(getEntityReference(reference).getSimpleName()).
                append(" t WHERE ");
        if (SchedTask.class.isAssignableFrom(reference)) {
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
    public <T extends Task> List<T> findToExec(final Class<T> reference) {
        StringBuilder queryString = buildFindAllQueryJPA(reference).append("AND ");

        if (NotificationTask.class.isAssignableFrom(reference)) {
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
    public <T extends Task> List<T> findAll(final Class<T> reference) {
        return findAll(reference, null, null, null, null, -1, -1, Collections.<OrderByClause>emptyList());
    }

    private <T extends Task> StringBuilder buildFindAllQuery(
            final Class<T> reference,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final boolean orderByTaskExecInfo,
            final List<Object> queryParameters) {

        if (resource != null
                && !PropagationTask.class.isAssignableFrom(reference)
                && !PushTask.class.isAssignableFrom(reference)
                && !PullTask.class.isAssignableFrom(reference)) {

            throw new IllegalArgumentException(reference.getSimpleName() + " is not related to "
                    + ExternalResource.class.getSimpleName());
        }

        if ((anyTypeKind != null || entityKey != null)
                && !PropagationTask.class.isAssignableFrom(reference)
                && !NotificationTask.class.isAssignableFrom(reference)) {

            throw new IllegalArgumentException(reference.getSimpleName()
                    + " is not related to users, groups or any objects");
        }

        if (notification != null && !NotificationTask.class.isAssignableFrom(reference)) {
            throw new IllegalArgumentException(reference.getSimpleName() + " is not related to notifications");
        }

        String tableName = getEntityTableName(reference);
        StringBuilder queryString = new StringBuilder("SELECT ").append(tableName).append(".*");

        if (orderByTaskExecInfo) {
            queryString.append(",").append(JPATaskExec.TABLE).append(".startDate AS startDate").
                    append(",").append(JPATaskExec.TABLE).append(".endDate AS endDate").
                    append(",").append(JPATaskExec.TABLE).append(".status AS status").
                    append(" FROM ").append(tableName).
                    append(",").append(JPATaskExec.TABLE).append(",").append("(SELECT ").
                    append(JPATaskExec.TABLE).append(".task_id, ").
                    append("MAX(").append(JPATaskExec.TABLE).append(".startDate) AS startDate").
                    append(" FROM ").append(JPATaskExec.TABLE).
                    append(" GROUP BY ").append(JPATaskExec.TABLE).append(".task_id) GRP").
                    append(" WHERE ").
                    append(tableName).append(".id=").append(JPATaskExec.TABLE).append(".task_id").
                    append(" AND ").append(tableName).append(".id=").append("GRP.task_id").
                    append(" AND ").append(JPATaskExec.TABLE).append(".startDate=").append("GRP.startDate");
        } else {
            queryString.
                    append(", null AS startDate, null AS endDate, null AS status FROM ").
                    append(tableName).
                    append(" WHERE 1=1");
        }

        queryParameters.add(getEntityTableName(reference));
        if (SchedTask.class.isAssignableFrom(reference)) {
            queryString.append(" AND ").
                    append(tableName).
                    append(".id NOT IN (SELECT ").append(tableName).append(".id FROM ").
                    append(tableName).append(" WHERE ").
                    append(tableName).
                    append(".id NOT IN (SELECT id FROM ").
                    append(tableName).
                    append("))");
        }
        queryString.append(' ');

        if (resource != null) {
            queryParameters.add(resource.getKey());

            queryString.append(" AND ").
                    append(tableName).
                    append(".resource_id=?").append(queryParameters.size());
        }
        if (notification != null) {
            queryParameters.add(notification.getKey());

            queryString.append(" AND ").
                    append(tableName).
                    append(".notification_id=?").append(queryParameters.size());
        }
        if (anyTypeKind != null && entityKey != null) {
            queryParameters.add(anyTypeKind.name());
            queryParameters.add(entityKey);

            queryString.append(" AND ").
                    append(tableName).
                    append(".anyTypeKind=?").append(queryParameters.size() - 1).
                    append(" AND ").
                    append(tableName).
                    append(".entityKey=?").append(queryParameters.size());
        }

        return queryString;
    }

    private String toOrderByStatement(final Class<? extends Task> beanClass, final List<OrderByClause> orderByClauses) {

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
            final Class<T> reference,
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
                reference,
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
                            reference,
                            resource,
                            notification,
                            anyTypeKind,
                            entityKey,
                            false,
                            queryParameters)).
                    append(" AND id NOT IN ").
                    append("(SELECT task_id AS id FROM ").append(JPATaskExec.TABLE).append(")").
                    append(")) T");
        } else {
            queryString.insert(0, "SELECT T.id FROM (").append(") T");
        }

        queryString.append(toOrderByStatement(getEntityReference(reference), orderByClauses));

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
    public <T extends Task> int count(
            final Class<T> reference,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

        List<Object> queryParameters = new ArrayList<>();

        StringBuilder queryString =
                buildFindAllQuery(reference, resource, notification, anyTypeKind, entityKey, false, queryParameters);

        String tableName = getEntityTableName(reference);
        Query query = entityManager().createNativeQuery(StringUtils.replaceOnce(
                queryString.toString(),
                "SELECT " + tableName + ".*, null AS startDate, null AS endDate, null AS status",
                "SELECT COUNT(" + tableName + ".id)"));

        for (int i = 1; i <= queryParameters.size(); i++) {
            query.setParameter(i, queryParameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task> T save(final T task) {
        T merged = entityManager().merge(task);

        // propagate the event only for Propagation tasks
        if (merged instanceof PropagationTask) {
            publisher.publishEvent(new TaskCreatedUpdatedEvent<>(this, merged, AuthContextUtils.getDomain()));
        }
        return merged;
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
            remediationDAO.findByPullTask((PullTask) task).forEach(remediation -> {
                remediation.setPullTask(null);
            });
        }

        entityManager().remove(task);
        publisher.publishEvent(new TaskDeletedEvent(this, task.getKey(), AuthContextUtils.getDomain()));
    }

    @Override
    public <T extends Task> void deleteAll(final ExternalResource resource, final Class<T> reference) {
        findAll(reference, resource, null, null, null, -1, -1, Collections.<OrderByClause>emptyList()).
                stream().map(Entity::getKey).forEach(task -> delete(task));
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
