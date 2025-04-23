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
package org.apache.syncope.core.provisioning.java.job;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SyncopeTaskSchedulerTest extends AbstractTest {

    private static final Mutable<Integer> VALUE = new MutableObject<>(0);

    private static class TestJob extends Job {

        @Override
        protected void execute(final JobExecutionContext context) throws JobExecutionException {
            VALUE.setValue(1);
        }
    }

    @Autowired
    private SyncopeTaskScheduler scheduler;

    @Test
    public void schedule() {
        JobExecutionContext context = new JobExecutionContext(
                AuthContextUtils.getDomain(),
                TestJob.class.getSimpleName() + "_" + SecureRandomUtils.generateRandomUUID(),
                AuthContextUtils.getWho(),
                false);
        TestJob job = new TestJob();
        job.setContext(context);

        JobStatusDAO jobStatusDAO = mock(JobStatusDAO.class);
        when(jobStatusDAO.lock(anyString())).thenReturn(true);
        doNothing().when(jobStatusDAO).unlock(anyString());
        ReflectionTestUtils.setField(job, "jobStatusDAO", jobStatusDAO);

        scheduler.schedule(job, Instant.now().plusSeconds(5));

        assertTrue(scheduler.contains(AuthContextUtils.getDomain(), job.getContext().getJobName()));

        assertTrue(scheduler.getNextTrigger(AuthContextUtils.getDomain(), job.getContext().getJobName()).isPresent());

        await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> VALUE.getValue() == 1);
    }
}
