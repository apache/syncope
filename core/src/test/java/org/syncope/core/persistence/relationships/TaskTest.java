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
package org.syncope.core.persistence.relationships;

import java.util.Date;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecutionDAO;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;
    @Autowired
    private TaskExecutionDAO taskExecutionDAO;

    @Test
    public final void addTaskExecution() {
        Task task = taskDAO.find(1L);
        assertNotNull(task);

        int executionNumber = task.getExecutions().size();

        TaskExecution execution = new TaskExecution();
        execution.setTask(task);
        task.addExecution(execution);
        execution.setStartDate(new Date());

        task = taskDAO.save(task);

        taskDAO.flush();

        task = taskDAO.find(1L);
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecutions().size());
    }

    @Test
    public final void deleteTask() {
        taskDAO.delete(1L);

        taskDAO.flush();

        assertNull(taskDAO.find(1L));
        assertNull(taskExecutionDAO.find(1L));
    }

    @Test
    public final void deleteTaskExecution() {
        TaskExecution execution =
                taskExecutionDAO.find(1L);
        int executionNumber =
                execution.getTask().getExecutions().size();

        taskExecutionDAO.delete(1L);

        taskExecutionDAO.flush();

        assertNull(taskExecutionDAO.find(1L));

        Task task = taskDAO.find(1L);
        assertEquals(task.getExecutions().size(),
                executionNumber - 1);
    }
}
