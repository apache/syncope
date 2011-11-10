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

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.types.PropagationMode;
import org.syncope.core.propagation.PropagationTaskExecStatus;
import org.syncope.types.PropagationOperation;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void read() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        assertNotNull(task.getExecs());
        assertFalse(task.getExecs().isEmpty());
        assertEquals(1, task.getExecs().size());
    }

    @Test
    public final void save() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        PropagationTask task = new PropagationTask();
        task.setResource(resource);
        task.setPropagationMode(PropagationMode.ASYNC);
        task.setResourceOperationType(PropagationOperation.CREATE);
        task.setAccountId("one@two.com");

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1",
                "testValue2"));
        attributes.add(
                AttributeBuilder.buildPassword("password".toCharArray()));
        task.setAttributes(attributes);

        task = taskDAO.save(task);
        assertNotNull(task);

        PropagationTask actual = taskDAO.find(task.getId());
        assertEquals(task, actual);

        taskDAO.flush();

        resource = resourceDAO.find("ws-target-resource-1");
        assertTrue(taskDAO.findAll(resource, PropagationTask.class).
                contains(task));
    }

    @Test
    public final void addPropagationTaskExecution() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec execution = new TaskExec();
        execution.setTask(task);
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());
        task.addExec(execution);
        execution.setStartDate(new Date());

        task = taskDAO.save(task);

        taskDAO.flush();

        task = taskDAO.find(1L);
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public final void addSyncTaskExecution() {
        SyncTask task = taskDAO.find(4L);
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec execution = new TaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        task.addExec(execution);
        execution.setMessage("A message");

        task = taskDAO.save(task);

        taskDAO.flush();

        task = taskDAO.find(4L);
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public final void deleteTask() {
        taskDAO.delete(1L);

        taskDAO.flush();

        assertNull(taskDAO.find(1L));
        assertNull(taskExecDAO.find(1L));
    }

    @Test
    public final void deleteTaskExecution() {
        TaskExec execution = taskExecDAO.find(1L);
        int executionNumber = execution.getTask().getExecs().size();

        taskExecDAO.delete(1L);

        taskExecDAO.flush();

        assertNull(taskExecDAO.find(1L));

        PropagationTask task = taskDAO.find(1L);
        assertEquals(task.getExecs().size(),
                executionNumber - 1);
    }
}
