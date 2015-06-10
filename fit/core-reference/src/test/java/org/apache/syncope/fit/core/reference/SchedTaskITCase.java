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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.api.job.SyncJob;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SchedTaskITCase extends AbstractTaskITCase {

    @Test
    public void getJobClasses() {
        List<String> jobClasses = syncopeService.info().getTaskJobs();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<SchedTaskTO> tasks =
                taskService.list(TaskType.SCHEDULED, SyncopeClient.getListQueryBuilder().build());
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            if (!(task instanceof SchedTaskTO) || task instanceof SyncTaskTO || task instanceof PushTaskTO) {
                fail();
            }
        }
    }

    @Test
    public void update() {
        SchedTaskTO task = taskService.read(SCHED_TASK_ID);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setKey(5);
        taskMod.setCronExpression(null);

        taskService.update(taskMod);
        SchedTaskTO actual = taskService.read(taskMod.getKey());
        assertNotNull(actual);
        assertEquals(task.getKey(), actual.getKey());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void issueSYNCOPE144() {
        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobClassName(SyncJob.class.getName());

        Response response = taskService.create(task);
        SchedTaskTO actual = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        task = taskService.read(actual.getKey());
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        response = taskService.create(task);
        actual = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }

    @Test
    public void issueSYNCOPE660() {
        List<TaskExecTO> list = taskService.listJobs(JobStatusType.ALL);
        int old_size = list.size();

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE660");
        task.setDescription("issueSYNCOPE660 Description");
        task.setJobClassName(TestSampleJob.class.getName());

        Response response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);

        list = taskService.listJobs(JobStatusType.ALL);
        assertEquals(old_size + 1, list.size());

        taskService.actionJob(task.getKey(), JobAction.START);

        int i = 0, maxit = 50;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }

            list = taskService.listJobs(JobStatusType.RUNNING);

            assertNotNull(list);
            i++;
        } while (list.size() < 1 && i < maxit);

        assertEquals(1, list.size());
        assertEquals(task.getKey(), list.get(0).getTask());

        taskService.actionJob(task.getKey(), JobAction.STOP);

        i = 0;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }

            list = taskService.listJobs(JobStatusType.RUNNING);

            assertNotNull(list);
            i++;
        } while (list.size() >= 1 && i < maxit);

        assertTrue(list.isEmpty());
    }
}
