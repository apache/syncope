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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class AbstractExecutableLogicTest {

    private static class TestLogic extends AbstractExecutableLogic<AnyTypeClassTO> {

        TestLogic(
                final JobManager jobManager,
                final SyncopeTaskScheduler scheduler,
                final JobStatusDAO jobStatusDAO) {

            super(jobManager, scheduler, jobStatusDAO);
        }

        @Override
        protected JobReference getReference(final String jobName) {
            return new JobReference(org.apache.syncope.common.lib.types.JobType.TASK, jobName, jobName);
        }

        @Override
        public ExecTO execute(final ExecSpecs specs) {
            return null;
        }

        @Override
        public Page<ExecTO> listExecutions(
                final String key,
                final OffsetDateTime before,
                final OffsetDateTime after,
                final Pageable pageable) {

            return Page.empty(pageable);
        }

        @Override
        public List<ExecTO> listRecentExecutions(final int max) {
            return List.of();
        }

        @Override
        public ExecTO deleteExecution(final String executionKey) {
            return null;
        }

        @Override
        public List<BatchResponseItem> deleteExecutions(
                final String key,
                final OffsetDateTime before,
                final OffsetDateTime after) {

            return List.of();
        }

        @Override
        public JobTO getJob(final String key) {
            return null;
        }

        @Override
        public List<JobTO> listJobs() {
            return List.of();
        }

        @Override
        public void actionJob(final String key, final JobAction action) {
        }

        @Override
        protected AnyTypeClassTO resolveReference(
                final java.lang.reflect.Method method, final Object... args) throws UnresolvedReferenceException {

            throw new UnresolvedReferenceException();
        }
    }

    private TestLogic logic;

    @BeforeEach
    void setUp() {
        logic = new TestLogic(
                mock(JobManager.class), mock(SyncopeTaskScheduler.class), mock(JobStatusDAO.class));
    }

    @Test
    void listExecutions() {
        assertEquals(0, logic.listExecutions("key", null, null, Pageable.unpaged()).getTotalElements());
    }

    @Test
    void listRecentExecutions() {
        assertEquals(List.of(), logic.listRecentExecutions(10));
    }

    @Test
    void deleteExecutions() {
        assertEquals(List.of(), logic.deleteExecutions("key", null, null));
    }

    @Test
    void listJobs() {
        assertEquals(List.of(), logic.listJobs());
    }

    @Test
    void contract() {
        ExecSpecs specs = new ExecSpecs.Builder().key("key").build();

        assertNull(logic.execute(specs));
        assertNull(logic.deleteExecution("key"));
        assertNull(logic.getJob("key"));
        logic.actionJob("key", JobAction.START);
    }
}
