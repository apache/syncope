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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskExecQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PropagationTaskITCase extends AbstractTaskITCase {

    @Test
    public void paginatedList() {
        PagedResult<PropagationTaskTO> tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).build());
        assertNotNull(tasks);
        assertEquals(2, tasks.getResult().size());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(2).size(2).build());
        assertNotNull(tasks);
        assertEquals(2, tasks.getPage());
        assertEquals(2, tasks.getResult().size());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1000).size(2).build());
        assertNotNull(tasks);
        assertTrue(tasks.getResult().isEmpty());
    }

    @Test
    public void read() {
        final PropagationTaskTO taskTO = taskService.read(3L, true);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void bulkAction() {
        PagedResult<PropagationTaskTO> before = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).build());

        // create user with testdb resource
        UserTO userTO = UserITCase.getUniqueSampleTO("taskBulk@apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(userTO);

        List<PropagationTaskTO> after = new ArrayList<>(
                taskService.<PropagationTaskTO>list(new TaskQuery.Builder(TaskType.PROPAGATION).build()).
                getResult());
        after.removeAll(before.getResult());
        assertFalse(after.isEmpty());

        BulkAction bulkAction = new BulkAction();
        bulkAction.setType(BulkAction.Type.DELETE);

        for (PropagationTaskTO taskTO : after) {
            bulkAction.getTargets().add(String.valueOf(taskTO.getKey()));
        }

        taskService.bulk(bulkAction);

        assertFalse(taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).build()).getResult().
                containsAll(after));
    }

    @Test
    public void issueSYNCOPE741() {
        for (int i = 0; i < 3; i++) {
            taskService.execute(new ExecuteQuery.Builder().key(1L).build());
            taskService.execute(new ExecuteQuery.Builder().key(2L).build());
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }

        // check list
        PagedResult<AbstractTaskTO> tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).details(false).build());
        for (AbstractTaskTO item : tasks.getResult()) {
            assertTrue(item.getExecutions().isEmpty());
        }

        tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).details(true).build());
        for (AbstractTaskTO item : tasks.getResult()) {
            assertFalse(item.getExecutions().isEmpty());
        }

        // check read
        PropagationTaskTO task = taskService.read(1L, false);
        assertNotNull(task);
        assertEquals(1L, task.getKey(), 0);
        assertTrue(task.getExecutions().isEmpty());

        task = taskService.read(1L, true);
        assertNotNull(task);
        assertEquals(1L, task.getKey(), 0);
        assertFalse(task.getExecutions().isEmpty());

        // check list executions
        PagedResult<ExecTO> execs = taskService.listExecutions(
                new TaskExecQuery.Builder().key(1L).page(1).size(2).build());
        assertTrue(execs.getTotalCount() >= execs.getResult().size());
    }
}
