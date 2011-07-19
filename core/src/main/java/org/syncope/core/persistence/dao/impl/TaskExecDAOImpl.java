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

    @Override
    public <T extends Task> List<TaskExec> findAll(Class<T> reference) {
        Query query = entityManager.createQuery("SELECT e "
                + "FROM " + TaskExec.class.getSimpleName() + " e "
                + "WHERE e.task.class=:taskClass");
        query.setParameter("taskClass", reference.getSimpleName());
        return query.getResultList();
    }

    @Override
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
