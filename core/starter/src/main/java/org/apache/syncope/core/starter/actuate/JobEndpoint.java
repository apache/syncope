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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(id = "job")
public class JobEndpoint {

    protected final SyncopeTaskScheduler syncopeTaskScheduler;

    protected final JobStatusDAO jobStatusDAO;

    public JobEndpoint(final SyncopeTaskScheduler syncopeTaskScheduler, final JobStatusDAO jobStatusDAO) {
        this.syncopeTaskScheduler = syncopeTaskScheduler;
        this.jobStatusDAO = jobStatusDAO;
    }

    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();

        // first information about jobs defined in the Scheduler
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

        // then check if there are jobs not reconciled, e.g. reported by JobStatusDAO but not by Scheduler
        // (potentially running in another node of the cluster)
        Map<String, Object> unreconciled = new HashMap<>();

        status.keySet().forEach(domain -> {
            Set<JobStatus> jobStatuses = new HashSet<>(
                    AuthContextUtils.callAsAdmin(domain, () -> jobStatusDAO.findAll()));
            jobStatuses.removeIf(syncopeTaskScheduler.getJobNames(domain)::contains);

            if (!jobStatuses.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jobs =
                        (Map<String, Object>) unreconciled.computeIfAbsent(domain, d -> new HashMap<>());

                jobStatuses.forEach(notfound -> jobs.put(notfound.getKey(), notfound.getStatus()));
            }
        });
        if (!unreconciled.isEmpty()) {
            status.put("unreconciled", unreconciled);
        }

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

    @DeleteOperation
    public void forceUnlock(final @Selector String domain, final @Selector String jobName) {
        AuthContextUtils.runAsAdmin(domain, () -> jobStatusDAO.unlock(jobName));
    }
}
