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
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class TaskExecTest extends AbstractTest {

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Test
    public void findAll() {
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2015, 11, 18, 0, 0, 0);

        List<TaskExec> execs = taskExecDAO.findAll(task, calendar.getTime(), null, null, null);
        assertNotNull(execs);
        assertEquals(1, execs.size());
    }

    @Test
    public void findLatestStarted() {
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        TaskExec latestStarted = taskExecDAO.findLatestStarted(task);
        assertNotNull(latestStarted);
        assertEquals("e58ca1c7-178a-4012-8a71-8aa14eaf0655", latestStarted.getKey());
    }

    @Test
    public void issueSYNCOPE214() {
        PropagationTask task = taskDAO.find("1e697572-b896-484c-ae7f-0c8f63fcbc6c");
        assertNotNull(task);

        String faultyMessage = "A faulty message";
        faultyMessage = faultyMessage.replace('a', '\0');

        TaskExec exec = entityFactory.newEntity(TaskExec.class);
        exec.setStart(new Date());
        exec.setEnd(new Date());
        exec.setStatus(PropagationTaskExecStatus.SUCCESS.name());
        exec.setMessage(faultyMessage);

        task.add(exec);
        exec.setTask(task);

        exec = taskExecDAO.save(exec);
        assertNotNull(exec);

        assertEquals(faultyMessage.replace('\0', '\n'), exec.getMessage());
    }
}
