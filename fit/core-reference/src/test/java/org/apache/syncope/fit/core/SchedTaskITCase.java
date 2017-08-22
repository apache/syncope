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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.core.reference.TestSampleJobDelegate;
import org.junit.Test;

public class SchedTaskITCase extends AbstractTaskITCase {

    @Test
    public void getJobClasses() {
        Set<String> jobClasses = syncopeService.platform().getTaskJobs();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<SchedTaskTO> tasks =
                taskService.list(new TaskQuery.Builder(TaskType.SCHEDULED).build());
        assertFalse(tasks.getResult().isEmpty());
        tasks.getResult().stream().filter(
                task -> !(task instanceof SchedTaskTO) || task instanceof PullTaskTO || task instanceof PushTaskTO).
                forEachOrdered(item -> {
                    fail();
                });
    }

    @Test
    public void update() {
        SchedTaskTO task = taskService.read(SCHED_TASK_KEY, true);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setKey(SCHED_TASK_KEY);
        taskMod.setName(task.getName());
        taskMod.setCronExpression(null);

        taskService.update(taskMod);
        SchedTaskTO actual = taskService.read(taskMod.getKey(), true);
        assertNotNull(actual);
        assertEquals(task.getKey(), actual.getKey());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void deferred() {
        SchedTaskTO task = new SchedTaskTO();
        task.setActive(true);
        task.setName("deferred");
        task.setJobDelegateClassName(TestSampleJobDelegate.class.getName());

        Response response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);

        Date initial = new Date();
        Date later = DateUtils.addSeconds(initial, 2);

        taskService.execute(new ExecuteQuery.Builder().key(task.getKey()).startAt(later).build());

        int i = 0;
        int maxit = 50;

        // wait for completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            task = taskService.read(task.getKey(), true);

            assertNotNull(task);
            assertNotNull(task.getExecutions());

            i++;
        } while (task.getExecutions().isEmpty() && i < maxit);

        PagedResult<ExecTO> execs =
                taskService.listExecutions(new ExecQuery.Builder().key(task.getKey()).build());
        assertEquals(1, execs.getTotalCount());
        assertTrue(execs.getResult().get(0).getStart().after(initial));
        // round 1 sec for safety
        assertTrue(DateUtils.addSeconds(execs.getResult().get(0).getStart(), 1).after(later));
    }

    @Test
    public void issueSYNCOPE144() {
        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobDelegateClassName(TestSampleJobDelegate.class.getName());

        Response response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task = taskService.read(task.getKey(), true);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144_2", task.getName());
        assertEquals("issueSYNCOPE144 Description_2", task.getDescription());
    }

    @Test
    public void issueSYNCOPE660() {
        List<JobTO> jobs = taskService.listJobs();
        int old_size = jobs.size();

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE660");
        task.setDescription("issueSYNCOPE660 Description");
        task.setJobDelegateClassName(TestSampleJobDelegate.class.getName());

        Response response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);

        jobs = taskService.listJobs();
        assertEquals(old_size + 1, jobs.size());

        taskService.actionJob(task.getKey(), JobAction.START);

        int i = 0, maxit = 50;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }

            jobs = taskService.listJobs().stream().filter(job -> job.isRunning()).collect(Collectors.toList());
            i++;
        } while (jobs.size() < 1 && i < maxit);

        assertEquals(1, jobs.size());
        assertEquals(task.getKey(), jobs.get(0).getRefKey());

        taskService.actionJob(task.getKey(), JobAction.STOP);

        i = 0;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }

            jobs = taskService.listJobs().stream().filter(job -> job.isRunning()).collect(Collectors.toList());
            i++;
        } while (jobs.size() >= 1 && i < maxit);

        assertTrue(jobs.isEmpty());
    }
}
