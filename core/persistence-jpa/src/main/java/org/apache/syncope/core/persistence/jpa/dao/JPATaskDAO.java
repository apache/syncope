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
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATask;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPATaskDAO extends AbstractDAO<Task, Long> implements TaskDAO {

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

            case SYNCHRONIZATION:
                result = JPASyncTask.class;
                break;

            default:
        }

        return result;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task> T find(final Long key) {
        return (T) entityManager.find(JPATask.class, key);
    }

    private <T extends Task> StringBuilder buildfindAllQuery(final TaskType type) {
        return new StringBuilder("SELECT e FROM ").
                append(getEntityReference(type).getSimpleName()).
                append(" e WHERE e.type=:type ");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> List<T> findToExec(final TaskType type) {
        StringBuilder queryString = buildfindAllQuery(type).append("AND ");

        if (type == TaskType.NOTIFICATION) {
            queryString.append("e.executed = 0 ");
        } else {
            queryString.append("e.executions IS EMPTY ");
        }
        queryString.append("ORDER BY e.id DESC");

        Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> List<T> findAll(final ExternalResource resource, final TaskType type) {
        StringBuilder queryString = buildfindAllQuery(type).append("AND e.resource=:resource ORDER BY e.id DESC");

        final Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("type", type);
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public <T extends Task> List<T> findAll(final TaskType type) {
        return findAll(-1, -1, Collections.<OrderByClause>emptyList(), type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> List<T> findAll(final int page, final int itemsPerPage,
            final List<OrderByClause> orderByClauses, final TaskType type) {

        StringBuilder queryString = buildfindAllQuery(type);
        queryString.append(orderByClauses.isEmpty()
                ? "ORDER BY e.id DESC"
                : toOrderByStatement(getEntityReference(type), "e", orderByClauses));

        Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("type", type);

        query.setFirstResult(itemsPerPage * (page <= 0
                ? 0
                : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public int count(final TaskType type) {
        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(id) FROM Task WHERE TYPE=?1");
        countQuery.setParameter(1, type.name());
        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task> T save(final T task) {
        return entityManager.merge(task);
    }

    @Override
    public void delete(final Long id) {
        Task task = find(id);
        if (task == null) {
            return;
        }

        delete(task);
    }

    @Override
    public void delete(final Task task) {
        entityManager.remove(task);
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        CollectionUtils.forAllDo(findAll(resource, type), new Closure<Task>() {

            @Override
            public void execute(final Task input) {
                delete(input.getKey());
            }
        });
    }
}
