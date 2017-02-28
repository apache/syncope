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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void findWithoutExecs() {
        List<PropagationTask> tasks = taskDAO.findToExec(TaskType.PROPAGATION);
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
    }

    @Test
    public void findPaginated() {
        List<Task> tasks = taskDAO.findAll(
                TaskType.PROPAGATION, null, null, null, null, 1, 2, Collections.<OrderByClause>emptyList());
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        for (Task task : tasks) {
            assertNotNull(task);
        }

        tasks = taskDAO.findAll(
                TaskType.PROPAGATION, null, null, null, null, 2, 2, Collections.<OrderByClause>emptyList());
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        for (Task task : tasks) {
            assertNotNull(task);
        }

        tasks = taskDAO.findAll(
                TaskType.PROPAGATION, null, null, null, null, 1000, 2, Collections.<OrderByClause>emptyList());
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());

        assertEquals(5, taskDAO.count(TaskType.PROPAGATION, null, null, null, null));
    }

    @Test
    public void findAll() {
        assertEquals(5, taskDAO.findAll(TaskType.PROPAGATION).size());
        assertEquals(1, taskDAO.findAll(TaskType.NOTIFICATION).size());
        assertEquals(3, taskDAO.findAll(TaskType.SCHEDULED).size());
        assertEquals(10, taskDAO.findAll(TaskType.PULL).size());
        assertEquals(11, taskDAO.findAll(TaskType.PUSH).size());
    }

    @Test
    public void savePropagationTask() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        User user = userDAO.find("74cd8ece-715a-44a4-a736-e17b46c4e7e6");
        assertNotNull(user);

        PropagationTask task = entityFactory.newEntity(PropagationTask.class);
        task.setResource(resource);
        task.setAnyTypeKind(AnyTypeKind.USER);
        task.setAnyType(AnyTypeKind.USER.name());
        task.setOperation(ResourceOperation.CREATE);
        task.setConnObjectKey("one@two.com");

        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1", "testValue2"));
        attributes.add(AttributeBuilder.buildPassword("password".toCharArray()));
        task.setAttributes(attributes);

        task = taskDAO.save(task);
        assertNotNull(task);

        PropagationTask actual = taskDAO.find(task.getKey());
        assertEquals(task, actual);
    }

    @Test
    public void delete() {
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        ExternalResource resource = task.getResource();
        assertNotNull(resource);

        taskDAO.delete(task);
        task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNull(task);

        resource = resourceDAO.find(resource.getKey());
        assertNotNull(resource);
        assertFalse(taskDAO.findAll(
                TaskType.PROPAGATION, resource, null, null, null, -1, -1, Collections.<OrderByClause>emptyList()).
                contains(task));
    }
}
