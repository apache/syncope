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
package org.apache.syncope.core.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SyncTaskTest extends AbstractDAOTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ResourceDAO resourceDAO;

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
