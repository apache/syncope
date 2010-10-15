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
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.persistence.dao.TaskExecutionDAO;

@Repository
public class TaskExecutionDAOImpl extends AbstractDAOImpl
        implements TaskExecutionDAO {

    @Override
    @Transactional(readOnly = true)
    public TaskExecution find(final Long id) {
        return entityManager.find(TaskExecution.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskExecution> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM TaskExecution e");
        return query.getResultList();
    }

    @Override
    public TaskExecution save(final TaskExecution execution) {
        return entityManager.merge(execution);
    }

    @Override
    public void delete(final Long id) {
        TaskExecution execution = find(id);
        if (execution == null) {
            return;
        }

        delete(execution);
    }

    @Override
    public void delete(final TaskExecution execution) {
        if (execution.getTask() != null) {
            execution.getTask().removeExecution(execution);
        }

        entityManager.remove(execution);
    }
}
