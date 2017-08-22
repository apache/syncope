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

import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.AbstractTask;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

@Repository
public class JPATaskDAO extends AbstractDAO<Task> implements TaskDAO {

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

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task> T find(final String key) {
        return (T) entityManager().find(AbstractTask.class, key);
    }

    private <T extends Task> StringBuilder buildFindAllQuery(final TaskType type) {
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
        StringBuilder queryString = buildFindAllQuery(type).append("AND ");

        if (type == TaskType.NOTIFICATION) {
            queryString.append("t.executed = 0 ");
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
        return findAll(type, null, null, null, null, -1, -1, Collections.<OrderByClause>emptyList());
    }

    private StringBuilder buildFindAllQuery(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

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

        StringBuilder queryString = buildFindAllQuery(type);

        if (resource != null) {
            queryString.append("AND t.resource=:resource ");
        }
        if (notification != null) {
            queryString.append("AND t.notification=:notification ");
        }
        if (anyTypeKind != null && entityKey != null) {
            queryString.append("AND t.anyTypeKind=:anyTypeKind AND t.entityKey=:entityKey ");
        }

        return queryString;
    }

    private String toOrderByStatement(
            final Class<? extends Task> beanClass, final List<OrderByClause> orderByClauses) {

        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(beanClass, field) != null) {
                statement.append("t.").append(field).append(' ').append(clause.getDirection().name());
            }
        });

        if (statement.length() == 0) {
            statement.append("ORDER BY t.id DESC");
        } else {
            statement.insert(0, "ORDER BY ");
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

        StringBuilder queryString = buildFindAllQuery(type, resource, notification, anyTypeKind, entityKey).
                append(toOrderByStatement(getEntityReference(type), orderByClauses));

        Query query = entityManager().createQuery(queryString.toString());
        if (resource != null) {
            query.setParameter("resource", resource);
        }
        if (notification != null) {
            query.setParameter("notification", notification);
        }
        if (anyTypeKind != null && entityKey != null) {
            query.setParameter("anyTypeKind", anyTypeKind);
            query.setParameter("entityKey", entityKey);
        }

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public int count(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

        StringBuilder queryString = buildFindAllQuery(type, resource, notification, anyTypeKind, entityKey);

        Query query = entityManager().createQuery(StringUtils.replaceOnce(
                queryString.toString(), "SELECT t", "SELECT COUNT(t)"));
        if (resource != null) {
            query.setParameter("resource", resource);
        }
        if (notification != null) {
            query.setParameter("notification", notification);
        }
        if (anyTypeKind != null && entityKey != null) {
            query.setParameter("anyTypeKind", anyTypeKind);
            query.setParameter("entityKey", entityKey);
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
        entityManager().remove(task);
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        findAll(type, resource, null, null, null, -1, -1, Collections.<OrderByClause>emptyList()).
                stream().map(Entity::getKey).forEach(task -> delete(task));
    }
}
