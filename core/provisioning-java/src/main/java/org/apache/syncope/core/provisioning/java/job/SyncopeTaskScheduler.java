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
import java.util.function.Function;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.StoppableSchedTaskJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class SyncopeTaskScheduler {

    protected record Key(String domain, String job) {

    }

    protected record Value(Job job, Optional<ScheduledFuture<?>> instant, Optional<ScheduledFuture<?>> cron) {

    }

    public static final String CACHE = "jobCache";

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeTaskScheduler.class);

    protected final TaskScheduler scheduler;

    protected final JobStatusDAO jobStatusDAO;

    protected final Map<Key, Value> jobs = new ConcurrentHashMap<>();

    public SyncopeTaskScheduler(final TaskScheduler scheduler, final JobStatusDAO jobStatusDAO) {
        this.scheduler = scheduler;
        this.jobStatusDAO = jobStatusDAO;
    }

    protected void register(final Job job, final ScheduledFuture<?> instant, final ScheduledFuture<?> cron) {
        Key key = new Key(job.getContext().getDomain(), job.getContext().getJobName());

        stop(key, instant == null ? List.of(Value::cron) : List.of(Value::instant));
        AuthContextUtils.runAsAdmin(key.domain(), () -> jobStatusDAO.unlock(key.job()));

        jobs.merge(
                key,
                new Value(job, Optional.ofNullable(instant), Optional.ofNullable(cron)),
                (prev, value) -> new Value(
                        job,
                        value.instant().isEmpty() ? prev.instant() : value.instant(),
                        value.cron().isEmpty() ? prev.cron() : value.cron()));
    }

    public void register(final Job job) {
        register(job, null, null);
    }

    public void schedule(final Job job, final Instant startTime) {
        register(job, scheduler.schedule(job, startTime), null);
    }

    public void schedule(final Job job, final CronTrigger trigger) {
        register(job, null, scheduler.schedule(job, trigger));
    }

    public void start(final String domain, final String jobName) {
        Optional.ofNullable(jobs.get(new Key(domain, jobName))).
                filter(value -> value.instant().map(Future::isDone).orElse(true)).
                ifPresent(value -> register(
                value.job(),
                scheduler.schedule(value.job(), Instant.now()),
                value.cron().orElse(null)));
    }

    protected void stop(final Key key, final List<Function<Value, Optional<ScheduledFuture<?>>>> suppliers) {
        Optional.ofNullable(jobs.get(key)).ifPresent(value -> {
            boolean mayInterruptIfRunning;
            if (value.job() instanceof TaskJob taskJob
                    && taskJob.getDelegate() instanceof StoppableSchedTaskJobDelegate stoppable) {

                stoppable.stop();
                mayInterruptIfRunning = false;
            } else {
                mayInterruptIfRunning = true;
            }

            suppliers.forEach(s -> s.apply(value).ifPresent(f -> f.cancel(mayInterruptIfRunning)));
        });
    }

    public void stop(final String domain, final String jobName) {
        stop(new Key(domain, jobName), List.of(Value::instant, Value::cron));
    }

    public void delete(final String domain, final String jobName) {
        jobs.remove(new Key(domain, jobName));
        AuthContextUtils.runAsAdmin(domain, () -> jobStatusDAO.unlock(jobName));
    }

    public Optional<Class<?>> getJobClass(final String domain, final String jobName) {
        return Optional.ofNullable(jobs.get(new Key(domain, jobName))).
                map(value -> AopUtils.getTargetClass(value.job()));
    }

    public Optional<OffsetDateTime> getNextTrigger(final String domain, final String jobName) {
        Value value = jobs.get(new Key(domain, jobName));
        if (value == null) {
            return Optional.empty();
        }

        ScheduledFuture<?> future = value.cron().filter(f -> !f.isDone()).orElse(null);
        if (future == null) {
            future = value.instant().filter(f -> !f.isDone()).orElse(null);
        }
        if (future == null) {
            return Optional.empty();
        }

        long delay = future.getDelay(TimeUnit.SECONDS);
        if (delay > 0) {
            return Optional.of(OffsetDateTime.now().plusSeconds(delay));
        }
        return Optional.empty();
    }

    public List<String> getJobNames(final String domain) {
        return jobs.keySet().stream().filter(key -> domain.equals(key.domain())).map(Key::job).toList();
    }
}
