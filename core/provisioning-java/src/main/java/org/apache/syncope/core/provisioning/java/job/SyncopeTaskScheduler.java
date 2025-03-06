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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.StoppableSchedTaskJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class SyncopeTaskScheduler {

    public static final String CACHE = "jobCache";

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeTaskScheduler.class);

    protected final TaskScheduler scheduler;

    protected final JobStatusDAO jobStatusDAO;

    protected final Map<Pair<String, String>, Pair<Job, Optional<ScheduledFuture<?>>>> jobs = new ConcurrentHashMap<>();

    public SyncopeTaskScheduler(final TaskScheduler scheduler, final JobStatusDAO jobStatusDAO) {
        this.scheduler = scheduler;
        this.jobStatusDAO = jobStatusDAO;
    }

    protected void register(final Job job, final Optional<ScheduledFuture<?>> future) {
        jobs.computeIfAbsent(
                Pair.of(job.getContext().getDomain(), job.getContext().getJobName()),
            k -> MutablePair.of(job, future));
    }

    public void register(final Job job) {
        register(job, Optional.empty());
    }

    public void schedule(final Job job, final CronTrigger trigger) {
        register(job, Optional.of(scheduler.schedule(job, trigger)));
    }

    public void schedule(final Job job, final Instant startTime) {
        register(job, Optional.of(scheduler.schedule(job, startTime)));
    }

    public boolean contains(final String domain, final String jobName) {
        return jobs.containsKey(Pair.of(domain, jobName));
    }

    public void start(final String domain, final String jobName) {
        Optional.ofNullable(jobs.get(Pair.of(domain, jobName))).
                filter(pair -> pair.getRight().map(Future::isDone).orElse(true)).
                ifPresent(pair -> pair.setValue(Optional.of(scheduler.schedule(pair.getLeft(), Instant.now()))));
    }

    public void cancel(final String domain, final String jobName) {
        Optional.ofNullable(jobs.get(Pair.of(domain, jobName))).ifPresent(pair -> {
            boolean mayInterruptIfRunning;
            if (pair.getLeft() instanceof TaskJob taskJob
                    && taskJob.getDelegate() instanceof StoppableSchedTaskJobDelegate stoppable) {

                stoppable.stop();
                mayInterruptIfRunning = false;
            } else {
                mayInterruptIfRunning = true;
            }

            pair.getRight().ifPresent(f -> f.cancel(mayInterruptIfRunning));
            pair.setValue(Optional.empty());
        });
    }

    public void delete(final String domain, final String jobName) {
        jobs.remove(Pair.of(domain, jobName));
        AuthContextUtils.runAsAdmin(domain, () -> jobStatusDAO.unlock(jobName));
    }

    public Optional<Class<?>> getJobClass(final String domain, final String jobName) {
        return Optional.ofNullable(jobs.get(Pair.of(domain, jobName))).
                map(pair -> AopUtils.getTargetClass(pair.getLeft()));
    }

    public Optional<OffsetDateTime> getNextTrigger(final String domain, final String jobName) {
        return Optional.ofNullable(jobs.get(Pair.of(domain, jobName))).
                filter(pair -> pair.getRight().map(f -> !f.isDone()).orElse(false)).
                flatMap(Pair::getRight).
                map(f -> f.getDelay(TimeUnit.SECONDS)).
                filter(delay -> delay > 0).
                map(delay -> OffsetDateTime.now().plusSeconds(delay));
    }

    public List<String> getJobNames(final String domain) {
        return jobs.keySet().stream().filter(pair -> domain.equals(pair.getLeft())).map(Pair::getRight).toList();
    }
}
