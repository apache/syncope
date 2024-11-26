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

import java.lang.management.ManagementFactory;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Reports about system load.
 */
public class SystemLoadReporterJob extends Job {

    protected static final Integer MB = 1024 * 1024;

    protected final ApplicationEventPublisher publisher;

    public SystemLoadReporterJob(final ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    protected void execute(final JobExecutionContext context) {
        SystemInfo.LoadInstant instant = new SystemInfo.LoadInstant();

        instant.setSystemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

        instant.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());

        Runtime runtime = Runtime.getRuntime();
        instant.setTotalMemory(runtime.totalMemory() / MB);
        instant.setMaxMemory(runtime.maxMemory() / MB);
        instant.setFreeMemory(runtime.freeMemory() / MB);

        publisher.publishEvent(instant);
    }
}
