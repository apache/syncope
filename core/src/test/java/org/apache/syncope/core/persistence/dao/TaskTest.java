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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
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
    private ResourceDAO resourceDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void findWithoutExecs() {
        List<PropagationTask> tasks = taskDAO.findToExec(PropagationTask.class);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
    }

    @Test
    public void findAll() {
        List<PropagationTask> plist = taskDAO.findAll(PropagationTask.class);
        assertEquals(4, plist.size());

        List<SchedTask> sclist = taskDAO.findAll(SchedTask.class);
        assertEquals(1, sclist.size());

        List<SyncTask> sylist = taskDAO.findAll(SyncTask.class);
        assertEquals(6, sylist.size());
    }

    @Test
    public void savePropagationTask() {
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
    }

    @Test
    public void delete() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        ExternalResource resource = task.getResource();
        assertNotNull(resource);

        taskDAO.delete(task);
        task = taskDAO.find(1L);
        assertNull(task);

        resource = resourceDAO.find(resource.getName());
        assertNotNull(resource);
        assertFalse(taskDAO.findAll(resource, PropagationTask.class).contains(task));
    }
}
