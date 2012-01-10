/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.TaskDAO;

@Repository
public class TaskDAOImpl extends AbstractDAOImpl
        implements TaskDAO {

    @Override
    @Transactional(readOnly = true)
    public <T extends Task> T find(final Long id) {
        return (T) entityManager.find(Task.class, id);
    }

    private <T extends Task> StringBuilder buildfindAllQuery(
            final Class<T> reference) {

        StringBuilder queryString =
                new StringBuilder("SELECT e FROM ").append(reference.
                getSimpleName()).append(" e ");
        if (SchedTask.class.equals(reference)) {
            queryString.append("WHERE e.id NOT IN (SELECT e.id FROM ").
                    append(SyncTask.class.getSimpleName()).append(" e) ");
        }

        return queryString;
    }

    @Override
    public <T extends Task> List<T> findWithoutExecs(final Class<T> reference) {
        StringBuilder queryString = buildfindAllQuery(reference);
        queryString.append("WHERE e.executions IS EMPTY");
        final Query query = entityManager.createQuery(queryString.toString());
        return query.getResultList();
    }

    @Override
    public <T extends Task> List<T> findAll(
            final ExternalResource resource, final Class<T> reference) {

        StringBuilder queryString = buildfindAllQuery(reference);
        if (SchedTask.class.equals(reference)) {
            queryString.append("AND ");
        } else {
            queryString.append("WHERE ");
        }
        queryString.append("e.resource=:resource");

        final Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public <T extends Task> List<T> findAll(final Class<T> reference) {
        return findAll(-1, -1, reference);
    }

    @Override
    public <T extends Task> List<T> findAll(final int page,
            final int itemsPerPage, final Class<T> reference) {

        final Query query = entityManager.createQuery(
                buildfindAllQuery(reference).toString());

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public List<PropagationTask> findAll(final SyncopeUser user) {
        StringBuilder queryString = buildfindAllQuery(PropagationTask.class);
        queryString.append("WHERE e.syncopeUser=:user");
        final Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("user", user);

        return query.getResultList();
    }

    @Override
    public List<PropagationTask> findAll(final ExternalResource resource,
            final SyncopeUser user) {

        StringBuilder queryString = buildfindAllQuery(PropagationTask.class);
        queryString.append("WHERE e.syncopeUser=:user ").
                append("AND e.resource=:resource");
        final Query query = entityManager.createQuery(queryString.toString());
        query.setParameter("user", user);
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public <T extends Task> Integer count(final Class<T> reference) {
        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(id) FROM Task WHERE DTYPE=?1");
        countQuery.setParameter(1, reference.getSimpleName());

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = {Throwable.class})
    @Override
    public <T extends Task> T save(final T task) {
        return entityManager.merge(task);
    }

    @Override
    public <T extends Task> void delete(final Long id) {
        T task = find(id);
        if (task == null) {
            return;
        }

        delete(task);
    }

    @Override
    public <T extends Task> void delete(final T task) {
        entityManager.remove(task);
    }

    @Override
    public <T extends Task> void deleteAll(
            final ExternalResource resource, final Class<T> reference) {

        List<T> tasks = findAll(resource, reference);
        if (tasks != null) {
            List<Long> taskIds = new ArrayList<Long>(tasks.size());
            for (T task : tasks) {
                taskIds.add(task.getId());
            }
            for (Long taskId : taskIds) {
                delete(taskId);
            }
        }
    }
}
