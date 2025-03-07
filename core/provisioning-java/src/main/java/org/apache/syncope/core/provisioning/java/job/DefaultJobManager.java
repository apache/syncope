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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.annotation.Transactional;

public class DefaultJobManager implements JobManager, SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(JobManager.class);

    protected final DomainHolder<?> domainHolder;

    protected final SyncopeTaskScheduler scheduler;

    protected final JobStatusDAO jobStatusDAO;

    protected final TaskDAO taskDAO;

    protected final ReportDAO reportDAO;

    protected final ImplementationDAO implementationDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final ConfParamOps confParamOps;

    protected final SecurityProperties securityProperties;

    public DefaultJobManager(
            final DomainHolder<?> domainHolder,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO,
            final TaskDAO taskDAO,
            final ReportDAO reportDAO,
            final ImplementationDAO implementationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final ConfParamOps confParamOps,
            final SecurityProperties securityProperties) {

        this.domainHolder = domainHolder;
        this.scheduler = scheduler;
        this.jobStatusDAO = jobStatusDAO;
        this.taskDAO = taskDAO;
        this.reportDAO = reportDAO;
        this.implementationDAO = implementationDAO;
        this.taskUtilsFactory = taskUtilsFactory;
        this.confParamOps = confParamOps;
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean isRunning(final String jobName) {
        synchronized (jobName) {
            boolean locked = jobStatusDAO.lock(jobName);
            if (locked) {
                jobStatusDAO.unlock(jobName);
            }
            return !locked;
        }
    }

    protected void registerJob(
            final JobExecutionContext context,
            final Class<? extends Job> jobClass,
            final String cronExpression,
            final OffsetDateTime startAt) {

        if (isRunning(context.getJobName())) {
            LOG.debug("Job {} already running, cancel", context.getJobName());
            return;
        }

        // 0. unregister job
        unregisterJob(context.getJobName());

        // 1. prepare job
        Job job = ApplicationContextProvider.getBeanFactory().createBean(jobClass);
        job.setContext(context);

        // 2. schedule
        if (cronExpression == null && startAt == null) {
            scheduler.register(job);
        } else {
            if (cronExpression == null) {
                scheduler.schedule(job, startAt.toInstant());
            } else {
                scheduler.schedule(job, new CronTrigger(cronExpression));
            }
        }
    }

    protected void register(
            final String domain,
            final SchedTask task,
            final OffsetDateTime startAt,
            final String executor,
            final boolean dryRun,
            final Map<String, Object> jobData) {

        Implementation jobDelegate = task.getJobDelegate() == null
                ? task instanceof PullTask
                        ? implementationDAO.findByType(IdRepoImplementationType.TASKJOB_DELEGATE).stream().
                                filter(impl -> PullJobDelegate.class.getName().equals(impl.getBody())).
                                findFirst().orElse(null)
                        : task instanceof PushTask
                                ? implementationDAO.findByType(IdRepoImplementationType.TASKJOB_DELEGATE).stream().
                                        filter(impl -> PushJobDelegate.class.getName().equals(impl.getBody())).
                                        findFirst().orElse(null)
                                : null
                : task.getJobDelegate();
        if (jobDelegate == null) {
            throw new IllegalArgumentException("Task " + task
                    + " does not provide any " + SchedTaskJobDelegate.class.getSimpleName());
        }

        JobExecutionContext context = new JobExecutionContext(
                domain,
                JobNamer.getJobName(task),
                executor,
                dryRun);
        context.getData().put(JobManager.TASK_TYPE, taskUtilsFactory.getInstance(task).getType());
        context.getData().put(JobManager.TASK_KEY, task.getKey());
        context.getData().put(JobManager.DELEGATE_IMPLEMENTATION, jobDelegate.getKey());
        context.getData().putAll(jobData);

        registerJob(
                context,
                TaskJob.class,
                task.getCronExpression(),
                startAt);
    }

    @Override
    public void register(
            final SchedTask task,
            final OffsetDateTime startAt,
            final String executor,
            final boolean dryRun,
            final Map<String, Object> jobData) {

        register(AuthContextUtils.getDomain(), task, startAt, executor, dryRun, jobData);
    }

    protected void register(
            final String domain,
            final Report report,
            final OffsetDateTime startAt,
            final String executor,
            final boolean dryRun) {

        JobExecutionContext context = new JobExecutionContext(
                domain,
                JobNamer.getJobName(report),
                executor,
                dryRun);
        context.getData().put(JobManager.REPORT_KEY, report.getKey());
        context.getData().put(JobManager.DELEGATE_IMPLEMENTATION, report.getJobDelegate().getKey());

        registerJob(
                context,
                ReportJob.class,
                report.getCronExpression(),
                startAt);
    }

    @Override
    public void register(
            final Report report,
            final OffsetDateTime startAt,
            final String executor,
            final boolean dryRun) {

        register(AuthContextUtils.getDomain(), report, startAt, executor, dryRun);
    }

    protected void unregisterJob(final String jobName) {
        scheduler.cancel(AuthContextUtils.getDomain(), jobName);
        scheduler.delete(AuthContextUtils.getDomain(), jobName);
    }

    @Override
    public void unregister(final Task<?> task) {
        unregisterJob(JobNamer.getJobName(task));
    }

    @Override
    public void unregister(final Report report) {
        unregisterJob(JobNamer.getJobName(report));
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Transactional
    @Override
    public void load(final String domain) {
        AuthContextUtils.runAsAdmin(domain, () -> {
            // 1. jobs for SchedTasks
            Set<SchedTask> tasks = new HashSet<>(taskDAO.findAll(TaskType.SCHEDULED));
            tasks.addAll(taskDAO.findAll(TaskType.PULL));
            tasks.addAll(taskDAO.findAll(TaskType.PUSH));
            tasks.addAll(taskDAO.findAll(TaskType.MACRO));
            tasks.addAll(taskDAO.findAll(TaskType.LIVE_SYNC));

            boolean loadException = false;
            for (Iterator<SchedTask> it = tasks.iterator(); it.hasNext() && !loadException;) {
                SchedTask task = it.next();

                LOG.debug("Loading job for {} Task {} {}",
                        taskUtilsFactory.getInstance(task).getType(), task.getKey(), task.getName());

                try {
                    register(domain, task, task.getStartAt(), securityProperties.getAdminUser(), false, Map.of());
                } catch (Exception e) {
                    LOG.error("While loading job instance for task {}", task.getKey(), e);
                    loadException = true;
                }
            }

            if (loadException) {
                LOG.error("Errors while loading job for tasks, aborting");
            } else {
                // 2. jobs for Reports
                for (Iterator<? extends Report> it = reportDAO.findAll().iterator(); it.hasNext() && !loadException;) {
                    Report report = it.next();

                    LOG.debug("Loading job for Report {} {}", report.getKey(), report.getName());

                    try {
                        register(domain, report, null, securityProperties.getAdminUser(), false);
                    } catch (Exception e) {
                        LOG.error("While loading job instance for report {}", report.getName(), e);
                        loadException = true;
                    }
                }

                if (loadException) {
                    LOG.error("Errors while loading job for reports, aborting");
                }
            }
        });

        if (SyncopeConstants.MASTER_DOMAIN.equals(domain)) {
            String notificationJobCronExp = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
                String result = StringUtils.EMPTY;

                String conf = confParamOps.get(
                        SyncopeConstants.MASTER_DOMAIN, "notificationjob.cronExpression", null, String.class);
                if (conf == null) {
                    result = NotificationJob.DEFAULT_CRON_EXP;
                } else if (!StringUtils.EMPTY.equals(conf)) {
                    result = conf;
                }
                return result;
            });

            // 3. NotificationJob
            if (StringUtils.isBlank(notificationJobCronExp)) {
                LOG.debug("Empty value provided for {}'s cron, not scheduling", NotificationJob.class.getSimpleName());
            } else {
                LOG.debug("{}'s cron expression: {} - scheduling",
                        NotificationJob.class.getSimpleName(), notificationJobCronExp);

                JobExecutionContext context = new JobExecutionContext(
                        domain,
                        NOTIFICATION_JOB,
                        securityProperties.getAdminUser(),
                        false);
                try {
                    registerJob(
                            context,
                            NotificationJob.class,
                            notificationJobCronExp,
                            null);
                } catch (Exception e) {
                    LOG.error("While loading {} instance", NotificationJob.class.getSimpleName(), e);
                }
            }

            // 4. SystemLoadReporterJob (fixed schedule, every minute)
            LOG.debug("Registering {}", SystemLoadReporterJob.class);

            JobExecutionContext context = new JobExecutionContext(
                    domain,
                    StringUtils.uncapitalize(SystemLoadReporterJob.class.getSimpleName()),
                    securityProperties.getAdminUser(),
                    false);
            try {
                registerJob(
                        context,
                        SystemLoadReporterJob.class,
                        "0 * * * * ?",
                        null);
            } catch (Exception e) {
                LOG.error("While loading {} instance", SystemLoadReporterJob.class.getSimpleName(), e);
            }
        }
    }

    @Override
    public void unload(final String domain) {
        AuthContextUtils.runAsAdmin(domain, () -> {
            // 1. jobs for SchedTasks
            Set<SchedTask> tasks = new HashSet<>(taskDAO.findAll(TaskType.SCHEDULED));
            tasks.addAll(taskDAO.findAll(TaskType.PULL));
            tasks.addAll(taskDAO.findAll(TaskType.PUSH));
            tasks.addAll(taskDAO.findAll(TaskType.MACRO));
            tasks.addAll(taskDAO.findAll(TaskType.LIVE_SYNC));

            tasks.forEach(task -> {
                LOG.debug("Unloading job for {} Task {} {}",
                        taskUtilsFactory.getInstance(task).getType(), task.getKey(), task.getName());

                try {
                    unregister(task);
                } catch (Exception e) {
                    LOG.error("While unloading job for task {}", task.getKey(), e);
                }
            });

            // 2. jobs for Reports
            reportDAO.findAll().forEach(report -> {
                LOG.debug("Unloading job for Report {} {}", report.getKey(), report.getName());

                try {
                    unregister(report);
                } catch (Exception e) {
                    LOG.error("While unloading job for report {}", report.getName(), e);
                }
            });
        });
    }
}
