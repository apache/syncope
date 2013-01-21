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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TaskTest extends AbstractDAOTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void read() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        assertNotNull(task.getExecs());
        assertFalse(task.getExecs().isEmpty());
        assertEquals(1, task.getExecs().size());
    }

    @Test
    public void save() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        SyncopeUser user = userDAO.find(2L);
        assertNotNull(user);

        PropagationTask task = new PropagationTask();
        task.setResource(resource);
        task.setSubjectType(AttributableType.USER);
        task.setPropagationMode(PropagationMode.TWO_PHASES);
        task.setPropagationOperation(ResourceOperation.CREATE);
        task.setAccountId("one@two.com");

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1", "testValue2"));
        attributes.add(AttributeBuilder.buildPassword("password".toCharArray()));
        task.setAttributes(attributes);

        task = taskDAO.save(task);
        assertNotNull(task);

        PropagationTask actual = taskDAO.find(task.getId());
        assertEquals(task, actual);

        taskDAO.flush();

        resource = resourceDAO.find("ws-target-resource-1");
        assertTrue(taskDAO.findAll(resource, PropagationTask.class).contains(task));
    }

    @Test
    public void addPropagationTaskExecution() {
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
    public void addSyncTaskExecution() {
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
    public void deleteTask() {
        taskDAO.delete(1L);

        taskDAO.flush();

        assertNull(taskDAO.find(1L));
        assertNull(taskExecDAO.find(1L));
    }

    @Test
    public void deleteTaskExecution() {
        TaskExec execution = taskExecDAO.find(1L);
        int executionNumber = execution.getTask().getExecs().size();

        taskExecDAO.delete(1L);

        taskExecDAO.flush();

        assertNull(taskExecDAO.find(1L));

        PropagationTask task = taskDAO.find(1L);
        assertEquals(task.getExecs().size(), executionNumber - 1);
    }
}
