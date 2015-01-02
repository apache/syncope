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
package org.apache.syncope.persistence.jpa.relationship;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.persistence.api.dao.TaskDAO;
import org.apache.syncope.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.persistence.api.dao.UserDAO;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.persistence.api.entity.task.PushTask;
import org.apache.syncope.persistence.api.entity.task.SyncTask;
import org.apache.syncope.persistence.api.entity.task.TaskExec;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.apache.syncope.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.persistence.jpa.entity.task.JPATaskExec;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

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

        User user = userDAO.find(2L);
        assertNotNull(user);

        PropagationTask task = new JPAPropagationTask();
        task.setResource(resource);
        task.setSubjectType(AttributableType.USER);
        task.setPropagationMode(PropagationMode.TWO_PHASES);
        task.setPropagationOperation(ResourceOperation.CREATE);
        task.setAccountId("one@two.com");

        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1", "testValue2"));
        attributes.add(AttributeBuilder.buildPassword("password".toCharArray()));
        task.setAttributes(attributes);

        task = taskDAO.save(task);
        assertNotNull(task);

        PropagationTask actual = taskDAO.find(task.getKey());
        assertEquals(task, actual);

        taskDAO.flush();

        resource = resourceDAO.find("ws-target-resource-1");
        assertTrue(taskDAO.findAll(resource, TaskType.PROPAGATION).contains(task));
    }

    @Test
    public void addPropagationTaskExecution() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec execution = new JPATaskExec();
        execution.setTask(task);
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());
        task.addExec(execution);
        execution.setStartDate(new Date());

        taskDAO.save(task);
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

        TaskExec execution = new JPATaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        task.addExec(execution);
        execution.setMessage("A message");

        taskDAO.save(task);
        taskDAO.flush();

        task = taskDAO.find(4L);
        assertNotNull(task);

        assertEquals(executionNumber + 1, task.getExecs().size());
    }

    @Test
    public void addPushTaskExecution() {
        PushTask task = taskDAO.find(13L);
        assertNotNull(task);

        int executionNumber = task.getExecs().size();

        TaskExec execution = new JPATaskExec();
        execution.setStatus("Text-free status");
        execution.setTask(task);
        task.addExec(execution);
        execution.setMessage("A message");

        taskDAO.save(task);
        taskDAO.flush();

        task = taskDAO.find(13L);
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
