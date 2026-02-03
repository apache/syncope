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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(id = "job")
public class JobEndpoint {

    protected final SyncopeTaskScheduler syncopeTaskScheduler;

    public JobEndpoint(final SyncopeTaskScheduler syncopeTaskScheduler) {
        this.syncopeTaskScheduler = syncopeTaskScheduler;
    }

    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();

        syncopeTaskScheduler.getJobs().forEach((k, v) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> jobs = (Map<String, Object>) status.computeIfAbsent(k.domain(), d -> new HashMap<>());

            Map<String, Object> job = new HashMap<>();
            jobs.put(k.job(), job);

            job.put("executor", v.job().getContext().getExecutor());
            job.put("dryRun", v.job().getContext().isDryRun());
            job.put("context", v.job().getContext().getData());

            v.instant().ifPresent(f -> job.put("delay (seconds)", f.getDelay(TimeUnit.SECONDS)));

            v.cron().ifPresent(f -> {
                job.put("next schedule (seconds)", f.getDelay(TimeUnit.SECONDS));
                job.put("done", f.isDone());
                job.put("cancelled", f.isCancelled());
            });
        });

        return status;
    }

    @WriteOperation
    public void action(
            final @Selector String domain,
            final @Selector String jobName,
            final @Selector JobAction action) {

        switch (action) {
            case START ->
                syncopeTaskScheduler.start(domain, jobName);

            case STOP ->
                syncopeTaskScheduler.stop(domain, jobName);

            case DELETE ->
                syncopeTaskScheduler.delete(domain, jobName);

            default -> {
            }
        }
    }
}
