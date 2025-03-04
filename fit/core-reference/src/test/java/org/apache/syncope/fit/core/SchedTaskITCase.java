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

import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.core.reference.TestSampleJobDelegate;
import org.junit.jupiter.api.Test;

public class SchedTaskITCase extends AbstractTaskITCase {

    @Test
    public void getJobClasses() {
        Set<String> jobClasses = ANONYMOUS_CLIENT.platform().
                getJavaImplInfo(IdRepoImplementationType.TASKJOB_DELEGATE).orElseThrow().getClasses();
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
        task.setName("deferred" + getUUIDString());
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        String taskKey = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(taskKey);

        OffsetDateTime initial = OffsetDateTime.now();
        OffsetDateTime later = initial.plusSeconds(2);

        TASK_SERVICE.execute(new ExecSpecs.Builder().key(taskKey).startAt(later).build());

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return !TASK_SERVICE.read(TaskType.SCHEDULED, taskKey, true).getExecutions().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });

        PagedResult<ExecTO> execs = TASK_SERVICE.listExecutions(new ExecQuery.Builder().key(taskKey).build());
        assertEquals(1, execs.getTotalCount());

        assertTrue(execs.getResult().getFirst().getStart().isAfter(initial));
        // round 1 sec for safety
        assertTrue(execs.getResult().getFirst().getStart().plusSeconds(1).isAfter(later));
    }

    @Test
    public void multistart() {
        ImplementationTO taskJobDelegate = IMPLEMENTATION_SERVICE.read(
                IdRepoImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setActive(true);
        task.setName("multistart" + getUUIDString());
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        String taskKey = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(taskKey);

        TASK_SERVICE.actionJob(taskKey, JobAction.START);

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return !TASK_SERVICE.read(TaskType.SCHEDULED, taskKey, true).getExecutions().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });

        PagedResult<ExecTO> execs = TASK_SERVICE.listExecutions(new ExecQuery.Builder().key(taskKey).build());
        assertEquals(1, execs.getTotalCount());

        TASK_SERVICE.actionJob(taskKey, JobAction.START);

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return TASK_SERVICE.read(TaskType.SCHEDULED, taskKey, true).getExecutions().size() >= 2;
            } catch (Exception e) {
                return false;
            }
        });

        execs = TASK_SERVICE.listExecutions(new ExecQuery.Builder().key(taskKey).build());
        assertEquals(2, execs.getTotalCount());
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
        int oldSize = TASK_SERVICE.listJobs().size();

        ImplementationTO taskJobDelegate = IMPLEMENTATION_SERVICE.read(
                IdRepoImplementationType.TASKJOB_DELEGATE, TestSampleJobDelegate.class.getSimpleName());
        assertNotNull(taskJobDelegate);

        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE660");
        task.setDescription("issueSYNCOPE660 Description");
        task.setJobDelegate(taskJobDelegate.getKey());

        Response response = TASK_SERVICE.create(TaskType.SCHEDULED, task);
        String taskKey = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class).getKey();

        assertEquals(oldSize + 1, TASK_SERVICE.listJobs().size());

        TASK_SERVICE.actionJob(taskKey, JobAction.START);

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return Optional.of(TASK_SERVICE.getJob(taskKey)).filter(JobTO::isRunning).isPresent();
            } catch (Exception e) {
                return false;
            }
        });

        TASK_SERVICE.actionJob(taskKey, JobAction.STOP);

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return Optional.of(TASK_SERVICE.getJob(taskKey)).filter(Predicate.not(JobTO::isRunning)).isPresent();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
