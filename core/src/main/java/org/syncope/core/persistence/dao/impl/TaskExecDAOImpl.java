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

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.TaskExecDAO;

@Repository
public class TaskExecDAOImpl extends AbstractDAOImpl
        implements TaskExecDAO {

    @Override
    public TaskExec find(final Long id) {
        return entityManager.find(TaskExec.class, id);
    }

    private <T extends Task> TaskExec findLatest(final T task,
            final String field) {

        Query query = entityManager.createQuery("SELECT e "
                + "FROM " + TaskExec.class.getSimpleName() + " e "
                + "WHERE e.task=:task "
                + "ORDER BY e." + field + " DESC");
        query.setParameter("task", task);
        query.setMaxResults(1);

        List<TaskExec> result = query.getResultList();
        return result == null || result.isEmpty()
                ? null : result.iterator().next();
    }

    @Override
    public <T extends Task> TaskExec findLatestStarted(final T task) {
        return findLatest(task, "startDate");
    }

    @Override
    public <T extends Task> TaskExec findLatestEnded(final T task) {
        return findLatest(task, "endDate");
    }

    @Override
    public <T extends Task> List<TaskExec> findAll(Class<T> reference) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(
                TaskExec.class.getSimpleName()).append(" e WHERE e.task IN (").
                append("SELECT t FROM ").append(reference.getSimpleName()).
                append(" t");
        if (SchedTask.class.equals(reference)) {
            queryString.append(" WHERE t.id NOT IN (SELECT t.id FROM ").
                    append(SyncTask.class.getSimpleName()).append(" t) ");
        }
        queryString.append(')');

        Query query = entityManager.createQuery(queryString.toString());
        return query.getResultList();
    }

    /**
     * This method has an explicit @Transactional annotation because it is 
     * called by AbstractJob.
     * 
     * @see org.syncope.core.scheduling.AbstractJob
     * 
     * @param execution entity to be merged
     * @return the same entity, updated
     */
    @Override
    @Transactional(rollbackFor = {Throwable.class})
    public TaskExec save(final TaskExec execution) {
        return entityManager.merge(execution);
    }

    @Override
    public void delete(final Long id) {
        TaskExec execution = find(id);
        if (execution == null) {
            return;
        }

        delete(execution);
    }

    @Override
    public void delete(final TaskExec execution) {
        if (execution.getTask() != null) {
            execution.getTask().removeExec(execution);
        }

        entityManager.remove(execution);
    }
}
