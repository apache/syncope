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
package org.apache.syncope.core.provisioning.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobKey;
import org.quartz.Scheduler;

public class JobNamerTest extends AbstractTest {

    private String name;

    @Test
    public void getTaskKeyFromJobName() {
        name = "testName";
        assertNull(JobNamer.getTaskKeyFromJobName(name));

        String uuid = UUID.randomUUID().toString();
        name = String.format("taskJob%s", uuid);
        assertEquals(uuid, JobNamer.getTaskKeyFromJobName(name));
    }

    @Test
    public void getReportKeyFromJobName() {
        name = "testName";
        assertNull(JobNamer.getTaskKeyFromJobName(name));

        String uuid = UUID.randomUUID().toString();
        name = String.format("reportJob%s", uuid);
        assertEquals(uuid, JobNamer.getReportKeyFromJobName(name));
    }

    @Test
    public void getJobKey(@Mock Task task) {
        String uuid = UUID.randomUUID().toString();
        when(task.getKey()).thenReturn(uuid);
        assertTrue(EqualsBuilder.reflectionEquals(new JobKey("taskJob" + task.getKey(), Scheduler.DEFAULT_GROUP),
                JobNamer.getJobKey(task)));
    }
    
    @Test
    public void getJobKey(@Mock Report report) {
        String uuid = UUID.randomUUID().toString();
        when(report.getKey()).thenReturn(uuid);
        assertTrue(EqualsBuilder.reflectionEquals(new JobKey("reportJob" + report.getKey(), Scheduler.DEFAULT_GROUP),
                JobNamer.getJobKey(report)));
    }
    
    @Test
    public void getTriggerName() {
        String jobName = "testJobName";
        assertEquals("Trigger_" + jobName, JobNamer.getTriggerName(jobName));
    }
}
