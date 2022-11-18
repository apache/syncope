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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.core.reference.TestSampleJobDelegate;
import org.junit.jupiter.api.Test;

public class SchedTaskITCase extends AbstractTaskITCase {

    @Test
    public void getJobClasses() {
        Set<String> jobClasses = ADMIN_CLIENT.platform().
                getJavaImplInfo(IdRepoImplementationType.TASKJOB_DELEGATE).get().getClasses();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<SchedTaskTO> tasks =
                TASK_SERVICE.search(new TaskQuery.Builder(TaskType.SCHEDULED).build());
        assertFalse(tasks.getResult().isEmpty());
        tasks.getResult().stream().filter(
                task -> !(task instanceof SchedTaskTO) || task instanceof PullTaskTO || task instanceof PushTaskTO).
                forEachOrdered(item -> fail("This should not happen"));
    }

    @Test
    public void update() {
        SchedTaskTO task = TASK_SERVICE.read(TaskType.SCHEDULED, SCHED_TASK_KEY, true);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setKey(SCHED_TASK_KEY);
        taskMod.setName(task.getName());
        taskMod.setCronExpression(null);

        TASK_SERVICE.update(TaskType.SCHEDULED, taskMod);
        SchedTaskTO actual = TASK_SERVICE.read(TaskType.SCHEDULED, taskMod.getKey(), true);
        assertNotNull(actual);
        assertEquals(task.getKey(), actual.getKey());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void deferred() {
        ImplementationTO taskJobDelegate = IMPLEMENTATION_SERVICE.read(
                IdRepoImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setActive(true);
        task.setName("deferred");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        String taskKey = task.getKey();
        assertNotNull(task);

        OffsetDateTime initial = OffsetDateTime.now();
        OffsetDateTime later = initial.plusSeconds(2);

        AtomicReference<TaskTO> taskTO = new AtomicReference<>(task);
        int preSyncSize = taskTO.get().getExecutions().size();
        ExecTO execution = TASK_SERVICE.execute(new ExecSpecs.Builder().key(task.getKey()).startAt(later).build());
        assertNotNull(execution.getExecutor());

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                taskTO.set(TASK_SERVICE.read(TaskType.SCHEDULED, taskKey, true));
                return preSyncSize < taskTO.get().getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });

        PagedResult<ExecTO> execs =
                TASK_SERVICE.listExecutions(new ExecQuery.Builder().key(task.getKey()).build());
        assertEquals(1, execs.getTotalCount());
        assertTrue(execs.getResult().get(0).getStart().isAfter(initial));
        // round 1 sec for safety
        assertTrue(execs.getResult().get(0).getStart().plusSeconds(1).isAfter(later));
    }

    @Test
    public void issueSYNCOPE144() {
        ImplementationTO taskJobDelegate = IMPLEMENTATION_SERVICE.read(
                IdRepoImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task = TASK_SERVICE.read(TaskType.SCHEDULED, task.getKey(), true);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(task);
        assertEquals("issueSYNCOPE144_2", task.getName());
        assertEquals("issueSYNCOPE144 Description_2", task.getDescription());
    }

    @Test
    public void issueSYNCOPE660() {
        List<JobTO> jobs = TASK_SERVICE.listJobs();
        int oldSize = jobs.size();

        ImplementationTO taskJobDelegate = IMPLEMENTATION_SERVICE.read(
                IdRepoImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE660");
        task.setDescription("issueSYNCOPE660 Description");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        task = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);

        jobs = TASK_SERVICE.listJobs();
        assertEquals(oldSize + 1, jobs.size());

        TASK_SERVICE.actionJob(task.getKey(), JobAction.START);

        AtomicReference<List<JobTO>> run = new AtomicReference<>();
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                run.set(TASK_SERVICE.listJobs().stream().filter(JobTO::isRunning).collect(Collectors.toList()));
                return !run.get().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
        assertEquals(1, run.get().size());
        assertEquals(task.getKey(), run.get().get(0).getRefKey());

        TASK_SERVICE.actionJob(task.getKey(), JobAction.STOP);

        run.set(List.of());
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                run.set(TASK_SERVICE.listJobs().stream().filter(JobTO::isRunning).collect(Collectors.toList()));
                return run.get().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
