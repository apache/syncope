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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TaskExecTest extends AbstractTest {

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskUtilsFactory taskUtilsFactory;

    @Test
    public void findAll() {
        PropagationTask task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        OffsetDateTime startedBefore = OffsetDateTime.of(2015, 12, 18, 0, 0, 0, 0, FormatUtils.DEFAULT_OFFSET);

        List<TaskExec<?>> execs = taskExecDAO.findAll(task, startedBefore, null, Pageable.unpaged());
        assertNotNull(execs);
        assertEquals(1, execs.size());
    }

    @Test
    public void findLatestStarted() {
        PropagationTask task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        TaskExec<?> latestStarted = taskExecDAO.findLatestStarted(TaskType.PROPAGATION, task).orElseThrow();
        assertEquals("e58ca1c7-178a-4012-8a71-8aa14eaf0655", latestStarted.getKey());
    }

    @Test
    public void issueSYNCOPE214() {
        PropagationTask task = (PropagationTask) taskDAO.findById(
                TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c").orElseThrow();
        assertNotNull(task);

        String faultyMessage = "A faulty message";
        faultyMessage = faultyMessage.replace('a', '\0');

        TaskExec<PropagationTask> exec = taskUtilsFactory.getInstance(TaskType.PROPAGATION).newTaskExec();
        exec.setStart(OffsetDateTime.now());
        exec.setEnd(OffsetDateTime.now());
        exec.setStatus(ExecStatus.SUCCESS.name());
        exec.setMessage(faultyMessage);
        exec.setExecutor("admin");

        task.add(exec);
        exec.setTask(task);

        exec = taskExecDAO.save(exec);
        assertNotNull(exec);

        assertEquals(faultyMessage.replace('\0', '\n'), exec.getMessage());
    }
}
