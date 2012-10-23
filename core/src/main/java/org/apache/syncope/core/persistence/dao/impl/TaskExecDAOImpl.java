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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TaskExecDAOImpl extends AbstractDAOImpl implements TaskExecDAO {

    @Autowired
    private TaskDAO taskDAO;

    @Override
    public TaskExec find(final Long id) {
        return entityManager.find(TaskExec.class, id);
    }

    private <T extends Task> TaskExec findLatest(final T task, final String field) {

        Query query = entityManager.createQuery("SELECT e " + "FROM " + TaskExec.class.getSimpleName() + " e "
                + "WHERE e.task=:task " + "ORDER BY e." + field + " DESC");
        query.setParameter("task", task);
        query.setMaxResults(1);

        List<TaskExec> result = query.getResultList();
        return result == null || result.isEmpty()
                ? null
                : result.iterator().next();
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
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(TaskExec.class.getSimpleName()).append(
                " e WHERE e.task IN (").append("SELECT t FROM ").append(reference.getSimpleName()).append(" t");
        if (SchedTask.class.equals(reference)) {
            queryString.append(" WHERE t.id NOT IN (SELECT t.id FROM ").append(SyncTask.class.getSimpleName()).append(
                    " t) ");
        }
        queryString.append(')');

        Query query = entityManager.createQuery(queryString.toString());
        return query.getResultList();
    }

    @Override
    public TaskExec save(final TaskExec execution) {
        return entityManager.merge(execution);
    }

    /**
     * This method has an explicit Transactional annotation because it is called by AbstractJob.
     *
     * @see AbstractJob
     *
     * @param taskId task id
     * @param execution task execution
     * @throws InvalidEntityException if any bean validation fails
     */
    @Override
    @Transactional(rollbackFor = {Throwable.class})
    public void saveAndAdd(final Long taskId, final TaskExec execution) throws InvalidEntityException {
        Task task = taskDAO.find(taskId);
        task.addExec(execution);
        taskDAO.save(task);
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
