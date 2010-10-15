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
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.dao.TaskDAO;

@Repository
public class TaskDAOImpl extends AbstractDAOImpl
        implements TaskDAO {

    @Override
    @Transactional(readOnly = true)
    public Task find(final Long id) {
        return entityManager.find(Task.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM Task e");
        return query.getResultList();
    }

    @Override
    public Task save(final Task task) {
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
        if (task.getResource() != null) {
            task.getResource().removeTask(task);
        }

        entityManager.remove(task);
    }
}
