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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.AbstractTest;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.quartz.TestSyncActions;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.PropagationOperation;
import org.apache.syncope.validation.InvalidEntityException;
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
    private ResourceDAO resourceDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void findWithoutExecs() {
        List<PropagationTask> tasks = taskDAO.findToExec(PropagationTask.class);
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
    }

    @Test
    public void findAll() {
        List<PropagationTask> plist = taskDAO.findAll(PropagationTask.class);
        assertEquals(4, plist.size());

        List<SchedTask> sclist = taskDAO.findAll(SchedTask.class);
        assertEquals(1, sclist.size());

        List<SyncTask> sylist = taskDAO.findAll(SyncTask.class);
        assertEquals(4, sylist.size());

        ExternalResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        SyncopeUser user = userDAO.find(1L);
        assertNotNull(user);

        plist = taskDAO.findAll(resource, user);
        assertEquals(3, plist.size());
    }

    @Test
    public void savePropagationTask() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        SyncopeUser user = userDAO.find(2L);
        assertNotNull(user);

        PropagationTask task = new PropagationTask();
        task.setResource(resource);
        task.setSyncopeUser(user);
        task.setPropagationMode(PropagationMode.TWO_PHASES);
        task.setPropagationOperation(PropagationOperation.CREATE);
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
    public void saveSyncTask() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        SyncTask task = new SyncTask();
        task.setName("saveSyncTask");
        task.setDescription("SyncTask description");
        task.setUserTemplate(new UserTO());
        task.setCronExpression("BLA BLA");

        // this save() fails because of an invalid Cron Expression
        InvalidEntityException exception = null;
        try {
            taskDAO.save(task);
        } catch (InvalidEntityException e) {
            exception = e;
        }
        assertNotNull(exception);

        task.setCronExpression(null);
        // this save() fails because a SyncTask requires a target resource
        exception = null;
        try {
            taskDAO.save(task);
        } catch (InvalidEntityException e) {
            exception = e;
        }
        assertNotNull(exception);

        task.setResource(resource);
        task.setActionsClassName(getClass().getName());

        // this save() fails because jobActionsClassName does not implement 
        // the right interface
        exception = null;
        try {
            taskDAO.save(task);
        } catch (InvalidEntityException e) {
            exception = e;
        }
        assertNotNull(exception);

        task.setActionsClassName(TestSyncActions.class.getName());
        // this save() finally works
        task = taskDAO.save(task);
        assertNotNull(task);

        SyncTask actual = taskDAO.find(task.getId());
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

    @Test
    public void issueSYNCOPE144() {
        ExternalResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        SyncTask task = new SyncTask();

        task.setResource(resource);
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setActionsClassName(TestSyncActions.class.getName());

        task = taskDAO.save(task);
        assertNotNull(task);

        SyncTask actual = taskDAO.find(task.getId());
        assertEquals(task, actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        actual.setName("issueSYNCOPE144_2");
        actual.setDescription("issueSYNCOPE144 Description_2");

        actual = taskDAO.save(actual);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }
}
