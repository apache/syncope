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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PropagationTaskITCase extends AbstractTaskITCase {

    @Test
    public void paginatedList() {
        PagedResult<PropagationTaskTO> tasks = taskService.list(
                TaskType.PROPAGATION,
                SyncopeClient.getListQueryBuilder().page(1).size(2).build());
        assertNotNull(tasks);
        assertEquals(2, tasks.getResult().size());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(
                TaskType.PROPAGATION,
                SyncopeClient.getListQueryBuilder().page(2).size(2).build());
        assertNotNull(tasks);
        assertEquals(2, tasks.getPage());
        assertEquals(2, tasks.getResult().size());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(
                TaskType.PROPAGATION,
                SyncopeClient.getListQueryBuilder().page(1000).size(2).build());
        assertNotNull(tasks);
        assertTrue(tasks.getResult().isEmpty());
    }

    @Test
    public void read() {
        final PropagationTaskTO taskTO = taskService.read(3L);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        TaskExecTO taskTO = taskService.readExecution(6L);
        assertNotNull(taskTO);
    }

    @Test
    public void deal() {
        // Currently test is not re-runnable.
        // To successfully run test second time it is necessary to restart cargo
        try {
            taskService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        TaskExecTO exec = taskService.execute(1L, false);
        assertEquals(PropagationTaskExecStatus.SUBMITTED.name(), exec.getStatus());

        ReportExecTO report = new ReportExecTO();
        report.setStatus(PropagationTaskExecStatus.SUCCESS.name());
        report.setMessage("OK");
        taskService.report(exec.getKey(), report);
        exec = taskService.readExecution(exec.getKey());
        assertEquals(PropagationTaskExecStatus.SUCCESS.name(), exec.getStatus());
        assertEquals("OK", exec.getMessage());

        taskService.delete(1L);
        try {
            taskService.readExecution(exec.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void issue196() {
        TaskExecTO exec = taskService.execute(6L, false);
        assertNotNull(exec);
        assertEquals(0, exec.getKey());
        assertNotNull(exec.getTask());
    }

    @Test
    public void bulkAction() {
        PagedResult<PropagationTaskTO> before = taskService.list(
                TaskType.PROPAGATION, SyncopeClient.getListQueryBuilder().build());

        // create user with testdb resource
        UserTO userTO = UserITCase.getUniqueSampleTO("taskBulk@apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(userTO);

        List<PropagationTaskTO> after = new ArrayList<>(
                taskService.<PropagationTaskTO>list(TaskType.PROPAGATION, SyncopeClient.getListQueryBuilder().build()).
                getResult());
        after.removeAll(before.getResult());
        assertFalse(after.isEmpty());

        BulkAction bulkAction = new BulkAction();
        bulkAction.setType(BulkAction.Type.DELETE);

        for (PropagationTaskTO taskTO : after) {
            bulkAction.getTargets().add(String.valueOf(taskTO.getKey()));
        }

        taskService.bulk(bulkAction);

        assertFalse(taskService.list(TaskType.PROPAGATION, SyncopeClient.getListQueryBuilder().build()).getResult().
                containsAll(after));
    }
}
