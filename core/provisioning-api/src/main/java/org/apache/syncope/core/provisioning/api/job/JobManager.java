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

import java.time.OffsetDateTime;
import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

@SuppressWarnings("squid:S1214")
public interface JobManager {

    String TASK_KEY = "task";

    String REPORT_KEY = "report";

    String DOMAIN_KEY = "domain";

    String EXECUTOR_KEY = "executor";

    JobKey NOTIFICATION_JOB = new JobKey("notificationJob", Scheduler.DEFAULT_GROUP);

    boolean isRunning(JobKey jobKey) throws SchedulerException;

    Map<String, Object> register(SchedTask task, OffsetDateTime startAt, long interruptMaxRetries, String executor)
            throws SchedulerException;

    void register(Report report, OffsetDateTime startAt, long interruptMaxRetries, String executor)
            throws SchedulerException;

    void unregister(Task task);

    void unregister(Report report);
}
