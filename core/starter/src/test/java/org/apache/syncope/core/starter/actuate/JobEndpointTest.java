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
package org.apache.syncope.core.starter.actuate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.java.job.Job;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class JobEndpointTest {

    private JobStatusDAO jobStatusDAO;

    private JobEndpoint endpoint;

    @BeforeEach
    void setUp() {
        jobStatusDAO = mock(JobStatusDAO.class);
    }

    private static Job job(final String jobName, final String executor, final boolean dryRun) {
        Job job = mock(Job.class);
        when(job.getContext()).thenReturn(
                new JobExecutionContext(
                        SyncopeConstants.MASTER_DOMAIN, jobName, executor, dryRun, Map.of("k", "v")));
        return job;
    }

    private static JobStatus jobStatus(final String key, final String status) {
        JobStatus jobStatus = mock(JobStatus.class);
        when(jobStatus.getKey()).thenReturn(key);
        when(jobStatus.getStatus()).thenReturn(status);
        return jobStatus;
    }

    @SuppressWarnings("unchecked")
    private static ScheduledFuture<?> scheduledFuture(
            final long delaySeconds, final boolean done, final boolean cancelled) {
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(future.getDelay(TimeUnit.SECONDS)).thenReturn(delaySeconds);
        when(future.isDone()).thenReturn(done);
        when(future.isCancelled()).thenReturn(cancelled);
        return future;
    }

    private static SyncopeTaskScheduler schedulerWith(
            final Map<SyncopeTaskScheduler.Key, SyncopeTaskScheduler.Value> jobs, final List<String> jobNames) {

        SyncopeTaskScheduler scheduler = mock(SyncopeTaskScheduler.class);
        when(scheduler.getJobs()).thenReturn(jobs);
        when(scheduler.getJobNames(SyncopeConstants.MASTER_DOMAIN)).thenReturn(jobNames);
        return scheduler;
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusReportsScheduledJobsAndUnreconciledJobStatuses() {
        ScheduledFuture<?> instant = scheduledFuture(42L, false, false);
        ScheduledFuture<?> cron = scheduledFuture(7L, true, false);
        SyncopeTaskScheduler scheduler = schedulerWith(
                Map.of(
                        new SyncopeTaskScheduler.Key(SyncopeConstants.MASTER_DOMAIN, "job1"),
                        new SyncopeTaskScheduler.Value(
                                job("job1", "executor1", true), Optional.of(instant), Optional.of(cron))),
                List.of("job1"));
        doReturn(List.of(jobStatus("job1", "Scheduled"), jobStatus("job2", "Running"))).
                when(jobStatusDAO).findAll();
        endpoint = new JobEndpoint(scheduler, jobStatusDAO);

        Map<String, Object> status;
        try (MockedStatic<AuthContextUtils> auth = mockStatic(AuthContextUtils.class)) {
            auth.when(() -> AuthContextUtils.callAsAdmin(anyString(), any(Callable.class))).
                    thenAnswer(invocation -> {
                        Callable<Object> callable = invocation.getArgument(1);
                        try {
                            return callable.call();
                        } catch (final Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
            status = endpoint.status();
        }

        Map<String, Object> domainJobs = (Map<String, Object>) status.get(SyncopeConstants.MASTER_DOMAIN);
        Map<String, Object> job = (Map<String, Object>) domainJobs.get("job1");
        assertEquals("executor1", job.get("executor"));
        assertEquals(Boolean.TRUE, job.get("dryRun"));
        assertEquals(Map.of("k", "v"), job.get("context"));
        assertEquals(42L, job.get("delay (seconds)"));
        assertEquals(7L, job.get("next schedule (seconds)"));
        assertEquals(Boolean.TRUE, job.get("done"));
        assertEquals(Boolean.FALSE, job.get("cancelled"));

        Map<String, Object> unreconciled = (Map<String, Object>) status.get("unreconciled");
        Map<String, Object> unreconciledJobs = (Map<String, Object>) unreconciled.get(SyncopeConstants.MASTER_DOMAIN);
        assertEquals("Running", unreconciledJobs.get("job2"));
        assertFalse(unreconciledJobs.containsKey("job1"));
    }

    @Test
    void statusWithoutJobsIsEmpty() {
        SyncopeTaskScheduler scheduler = schedulerWith(Map.of(), List.of());
        endpoint = new JobEndpoint(scheduler, jobStatusDAO);

        assertTrue(endpoint.status().isEmpty());
    }

    @Test
    void actionStartsStopsAndDeletesJobs() {
        SyncopeTaskScheduler scheduler = mock(SyncopeTaskScheduler.class);
        endpoint = new JobEndpoint(scheduler, jobStatusDAO);

        endpoint.action(SyncopeConstants.MASTER_DOMAIN, "job1", JobAction.START);
        verify(scheduler).start(SyncopeConstants.MASTER_DOMAIN, "job1");

        endpoint.action(SyncopeConstants.MASTER_DOMAIN, "job1", JobAction.STOP);
        verify(scheduler).stop(SyncopeConstants.MASTER_DOMAIN, "job1");

        endpoint.action(SyncopeConstants.MASTER_DOMAIN, "job1", JobAction.DELETE);
        verify(scheduler).delete(SyncopeConstants.MASTER_DOMAIN, "job1");
    }

    @Test
    void forceUnlockReleasesJobStatusLock() {
        SyncopeTaskScheduler scheduler = mock(SyncopeTaskScheduler.class);
        endpoint = new JobEndpoint(scheduler, jobStatusDAO);

        try (MockedStatic<AuthContextUtils> auth = mockStatic(AuthContextUtils.class)) {
            auth.when(() -> AuthContextUtils.runAsAdmin(anyString(), any(Runnable.class))).thenAnswer(ic -> {
                ic.getArgument(1, Runnable.class).run();
                return null;
            });
            endpoint.forceUnlock(SyncopeConstants.MASTER_DOMAIN, "job1");
        }

        verify(jobStatusDAO).unlock("job1");
    }
}
