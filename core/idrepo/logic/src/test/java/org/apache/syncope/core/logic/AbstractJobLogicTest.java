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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractJobLogicTest {

    private static class TestLogic extends AbstractJobLogic<AnyTypeClassTO> {

        TestLogic(
                final JobManager jobManager,
                final SyncopeTaskScheduler scheduler,
                final JobStatusDAO jobStatusDAO) {

            super(jobManager, scheduler, jobStatusDAO);
        }

        @Override
        protected JobReference getReference(final String jobName) {
            return "known".equals(jobName) ? new JobReference(JobType.TASK, "key", "desc") : null;
        }

        Optional<JobTO> jobTO(final String jobName, final boolean includeCustom) {
            return getJobTO(jobName, includeCustom);
        }

        List<JobTO> listJobs(final boolean includeCustom) {
            return doListJobs(includeCustom);
        }

        void actionJob(final String jobName, final JobAction action) {
            doActionJob(jobName, action);
        }

        @Override
        protected AnyTypeClassTO resolveReference(final java.lang.reflect.Method method, final Object... args)
                throws UnresolvedReferenceException {

            throw new UnresolvedReferenceException();
        }
    }

    private JobManager jobManager;

    private SyncopeTaskScheduler scheduler;

    private JobStatusDAO jobStatusDAO;

    private TestLogic logic;

    @BeforeEach
    void setUp() {
        jobManager = mock(JobManager.class);
        scheduler = mock(SyncopeTaskScheduler.class);
        jobStatusDAO = mock(JobStatusDAO.class);
        logic = new TestLogic(jobManager, scheduler, jobStatusDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void knownJob() {
        OffsetDateTime nextTrigger = OffsetDateTime.now().plusHours(1);
        when(scheduler.getNextTrigger("Master", "known")).thenReturn(Optional.of(nextTrigger));
        when(jobManager.isRunning("known")).thenReturn(true);
        when(jobStatusDAO.get("known")).thenReturn("status");

        JobTO job = logic.jobTO("known", false).orElseThrow();

        assertEquals(JobType.TASK, job.getType());
        assertEquals("key", job.getRefKey());
        assertEquals("desc", job.getRefDesc());
        assertTrue(job.isScheduled());
        assertSame(nextTrigger, job.getStart());
        assertTrue(job.isRunning());
        assertEquals("status", job.getStatus());
    }

    @Test
    void knownJobNotScheduled() {
        when(scheduler.getNextTrigger("Master", "known")).thenReturn(Optional.empty());
        when(jobManager.isRunning("known")).thenReturn(false);

        JobTO job = logic.jobTO("known", false).orElseThrow();

        assertFalse(job.isScheduled());
        assertFalse(job.isRunning());
    }

    @Test
    void unknownJobWithoutCustom() {
        assertTrue(logic.jobTO("unknown", false).isEmpty());
    }

    @Test
    void unknownCustomJob() {
        when(scheduler.getJobClass("Master", "custom")).thenReturn(Optional.of(String.class));
        when(scheduler.getNextTrigger("Master", "custom")).thenReturn(Optional.empty());
        when(jobManager.isRunning("custom")).thenReturn(false);

        JobTO job = logic.jobTO("custom", true).orElseThrow();

        assertEquals(JobType.CUSTOM, job.getType());
        assertEquals("custom", job.getRefKey());
        assertEquals(String.class.getName(), job.getRefDesc());
    }

    @Test
    void unknownCustomJobWithBuiltInClass() {
        when(scheduler.getJobClass("Master", "builtIn")).
                thenReturn(Optional.of(org.apache.syncope.core.provisioning.java.job.TaskJob.class));

        assertTrue(logic.jobTO("builtIn", true).isEmpty());
    }

    @Test
    void listJobs() {
        when(scheduler.getJobNames("Master")).thenReturn(List.of("known", "unknown"));
        when(scheduler.getNextTrigger("Master", "known")).thenReturn(Optional.empty());

        assertEquals(List.of("key"), logic.listJobs(false).stream().map(JobTO::getRefKey).toList());
    }

    @Test
    void actionJob() {
        logic.actionJob("known", JobAction.START);
        logic.actionJob("known", JobAction.STOP);
        logic.actionJob("known", JobAction.DELETE);

        verify(scheduler).start("Master", "known");
        verify(scheduler).stop("Master", "known");
        verify(scheduler).delete("Master", "known");
    }
}
