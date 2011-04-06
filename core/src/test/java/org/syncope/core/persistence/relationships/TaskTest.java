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
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecutionDAO;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecutionDAO taskExecutionDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void save() {
        TargetResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        Task task = new Task();
        task.setResource(resource);
        task.setPropagationMode(PropagationMode.ASYNC);
        task.setResourceOperationType(ResourceOperationType.CREATE);
        task.setAccountId("one@two.com");

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1",
                "testValue2"));
        attributes.add(
                AttributeBuilder.buildPassword("password".toCharArray()));
        task.setAttributes(attributes);

        task = taskDAO.save(task);
        assertNotNull(task);

        Task actual = taskDAO.find(task.getId());
        assertEquals(task, actual);

        taskDAO.flush();

        resource = resourceDAO.find("ws-target-resource-1");
        assertTrue(resource.getTasks().contains(task));
    }

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
