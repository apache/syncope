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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TaskExecTest extends AbstractDAOTest {

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Test
    public void findAll() {
        List<TaskExec> list = taskExecDAO.findAll(PropagationTask.class);
        assertEquals(2, list.size());

        list = taskExecDAO.findAll(SchedTask.class);
        assertEquals(0, list.size());

        list = taskExecDAO.findAll(SyncTask.class);
        assertEquals(0, list.size());

        list = taskExecDAO.findAll(NotificationTask.class);
        // Notification task executions existence depends on the execution time of the NotificationJob.
        // Notification task executio list could be empty or not ....
        assertTrue(list.size() >= 0);
    }

    @Test
    public void findLatestStarted() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        TaskExec latestStarted = taskExecDAO.findLatestStarted(task);
        assertNotNull(latestStarted);
        assertEquals(Long.valueOf(1L), latestStarted.getId());
    }

    @Test
    public void issueSYNCOPE214() {
        PropagationTask task = taskDAO.find(1L);
        assertNotNull(task);

        String faultyMessage = "A faulty message";
        faultyMessage = faultyMessage.replace('a', '\0');

        TaskExec exec = new TaskExec();
        exec.setStartDate(new Date());
        exec.setEndDate(new Date());
        exec.setStatus(PropagationTaskExecStatus.SUCCESS.name());
        exec.setMessage(faultyMessage);

        task.addExec(exec);
        exec.setTask(task);

        exec = taskExecDAO.save(exec);
        assertNotNull(exec);

        assertEquals(faultyMessage.replace('\0', '\n'), exec.getMessage());
    }
}
