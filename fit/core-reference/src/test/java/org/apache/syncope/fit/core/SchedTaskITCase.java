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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.core.reference.TestSampleJobDelegate;
import org.junit.jupiter.api.Test;

public class SchedTaskITCase extends AbstractTaskITCase {

    @Test
    public void getJobClasses() {
        Set<String> jobClasses = syncopeService.platform().
                getJavaImplInfo(ImplementationType.TASKJOB_DELEGATE).get().getClasses();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<SchedTaskTO> tasks =
                taskService.search(new TaskQuery.Builder(TaskType.SCHEDULED).build());
        assertFalse(tasks.getResult().isEmpty());
        tasks.getResult().stream().filter(
                task -> !(task instanceof SchedTaskTO) || task instanceof PullTaskTO || task instanceof PushTaskTO).
                forEachOrdered(item -> {
                    fail("This should not happen");
                });
    }

    @Test
    public void update() {
        SchedTaskTO task = taskService.read(TaskType.SCHEDULED, SCHED_TASK_KEY, true);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setKey(SCHED_TASK_KEY);
        taskMod.setName(task.getName());
        taskMod.setCronExpression(null);

        taskService.update(TaskType.SCHEDULED, taskMod);
        SchedTaskTO actual = taskService.read(TaskType.SCHEDULED, taskMod.getKey(), true);
        assertNotNull(actual);
        assertEquals(task.getKey(), actual.getKey());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void deferred() {
        ImplementationTO taskJobDelegate = implementationService.read(
                ImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setActive(true);
        task.setName("deferred");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = taskService.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);

        Date initial = new Date();
        Date later = DateUtils.addSeconds(initial, 2);

        taskService.execute(new ExecuteQuery.Builder().key(task.getKey()).startAt(later).build());

        int i = 0;

        // wait for completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            task = taskService.read(TaskType.SCHEDULED, task.getKey(), true);

            assertNotNull(task);
            assertNotNull(task.getExecutions());

            i++;
        } while (task.getExecutions().isEmpty() && i < MAX_WAIT_SECONDS);

        PagedResult<ExecTO> execs =
                taskService.listExecutions(new ExecQuery.Builder().key(task.getKey()).build());
        assertEquals(1, execs.getTotalCount());
        assertTrue(execs.getResult().get(0).getStart().after(initial));
        // round 1 sec for safety
        assertTrue(DateUtils.addSeconds(execs.getResult().get(0).getStart(), 1).after(later));
    }

    @Test
    public void issueSYNCOPE144() {
        ImplementationTO taskJobDelegate = implementationService.read(
                ImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = taskService.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task = taskService.read(TaskType.SCHEDULED, task.getKey(), true);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        response = taskService.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144_2", task.getName());
        assertEquals("issueSYNCOPE144 Description_2", task.getDescription());
    }

    @Test
    public void issueSYNCOPE660() {
        List<JobTO> jobs = taskService.listJobs();
        int old_size = jobs.size();

        ImplementationTO taskJobDelegate = implementationService.read(
                ImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE660");
        task.setDescription("issueSYNCOPE660 Description");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = taskService.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);

        jobs = taskService.listJobs();
        assertEquals(old_size + 1, jobs.size());

        taskService.actionJob(task.getKey(), JobAction.START);

        int i = 0;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }

            jobs = taskService.listJobs().stream().filter(JobTO::isRunning).collect(Collectors.toList());
            i++;
        } while (jobs.size() < 1 && i < MAX_WAIT_SECONDS);

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
        } while (jobs.size() >= 1 && i < MAX_WAIT_SECONDS);

        assertTrue(jobs.isEmpty());
    }
}
