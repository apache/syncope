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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class SyncopeTaskScheduler {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeTaskScheduler.class);

    protected final TaskScheduler scheduler;

    protected final JobStatusDAO jobStatusDAO;

    protected final Map<Pair<String, String>, Pair<Job, ScheduledFuture<?>>> tasks = new ConcurrentHashMap<>();

    public SyncopeTaskScheduler(final TaskScheduler scheduler, final JobStatusDAO jobStatusDAO) {
        this.scheduler = scheduler;
        this.jobStatusDAO = jobStatusDAO;
    }

    public void register(final Job job) {
        tasks.put(
                Pair.of(job.getContext().getDomain(), job.getContext().getJobName()),
                Pair.of(job, null));
    }

    public void start(final String domain, final String jobName) {
        Optional.ofNullable(tasks.get(Pair.of(domain, jobName))).
                ifPresent(pair -> schedule(pair.getLeft(), Instant.now()));
    }

    public void schedule(final Job job, final CronTrigger trigger) {
        ScheduledFuture<?> future = scheduler.schedule(job, trigger);
        tasks.put(
                Pair.of(job.getContext().getDomain(), job.getContext().getJobName()),
                Pair.of(job, future));
    }

    public void schedule(final Job job, final Instant startTime) {
        ScheduledFuture<?> future = scheduler.schedule(job, startTime);
        tasks.put(
                Pair.of(job.getContext().getDomain(), job.getContext().getJobName()),
                Pair.of(job, future));
    }

    public boolean contains(final String domain, final String jobName) {
        return tasks.containsKey(Pair.of(domain, jobName));
    }

    public Optional<Class<?>> getJobClass(final String domain, final String jobName) {
        return Optional.ofNullable(tasks.get(Pair.of(domain, jobName))).
                map(pair -> AopUtils.getTargetClass(pair.getLeft()));
    }

    public Optional<OffsetDateTime> getNextTrigger(final String domain, final String jobName) {
        return Optional.ofNullable(tasks.get(Pair.of(domain, jobName))).
                filter(f -> f.getRight() != null).
                map(f -> f.getRight().getDelay(TimeUnit.SECONDS)).
                filter(delay -> delay > 0).
                map(delay -> OffsetDateTime.now().plusSeconds(delay));
    }

    public void cancel(final String domain, final String jobName) {
        Optional.ofNullable(tasks.get(Pair.of(domain, jobName))).
                filter(f -> f.getRight() != null).ifPresent(f -> f.getRight().cancel(true));
    }

    public void delete(final String domain, final String jobName) {
        tasks.remove(Pair.of(domain, jobName));
        AuthContextUtils.runAsAdmin(domain, () -> jobStatusDAO.unlock(jobName));
    }

    public List<String> getJobNames(final String domain) {
        return tasks.keySet().stream().filter(pair -> domain.equals(pair.getLeft())).map(Pair::getRight).toList();
    }
}
