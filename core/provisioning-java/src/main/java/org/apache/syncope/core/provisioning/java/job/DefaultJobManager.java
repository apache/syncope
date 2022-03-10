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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.identityconnectors.common.IOUtil;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;

public class DefaultJobManager implements JobManager, SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(JobManager.class);

    protected final DomainHolder domainHolder;

    protected final SchedulerFactoryBean scheduler;

    protected final TaskDAO taskDAO;

    protected final ReportDAO reportDAO;

    protected final ImplementationDAO implementationDAO;

    protected final ConfParamOps confParamOps;

    protected final SecurityProperties securityProperties;

    protected boolean disableQuartzInstance;

    public DefaultJobManager(
            final DomainHolder domainHolder,
            final SchedulerFactoryBean scheduler,
            final TaskDAO taskDAO,
            final ReportDAO reportDAO,
            final ImplementationDAO implementationDAO,
            final ConfParamOps confParamOps,
            final SecurityProperties securityProperties) {

        this.domainHolder = domainHolder;
        this.scheduler = scheduler;
        this.taskDAO = taskDAO;
        this.reportDAO = reportDAO;
        this.implementationDAO = implementationDAO;
        this.confParamOps = confParamOps;
        this.securityProperties = securityProperties;
    }

    public void setDisableQuartzInstance(final boolean disableQuartzInstance) {
        this.disableQuartzInstance = disableQuartzInstance;
    }

    protected boolean isRunningHere(final JobKey jobKey) throws SchedulerException {
        return scheduler.getScheduler().getCurrentlyExecutingJobs().stream().
                anyMatch(jec -> jobKey.equals(jec.getJobDetail().getKey()));
    }

    protected boolean isRunningElsewhere(final JobKey jobKey) throws SchedulerException {
        if (!scheduler.getScheduler().getMetaData().isJobStoreClustered()) {
            return false;
        }

        DataSource dataSource = domainHolder.getDomains().get(SyncopeConstants.MASTER_DOMAIN);
        Connection conn = DataSourceUtils.getConnection(dataSource);
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT 1 FROM " + Constants.DEFAULT_TABLE_PREFIX + "FIRED_TRIGGERS "
                    + "WHERE JOB_NAME = ? AND JOB_GROUP = ?");
            stmt.setString(1, jobKey.getName());
            stmt.setString(2, jobKey.getGroup());

            resultSet = stmt.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new SchedulerException(e);
        } finally {
            IOUtil.quietClose(resultSet);
            IOUtil.quietClose(stmt);
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public boolean isRunning(final JobKey jobKey) throws SchedulerException {
        return isRunningHere(jobKey) || isRunningElsewhere(jobKey);
    }

    protected void registerJob(
            final String jobName,
            final Class<? extends Job> jobClass,
            final String cronExpression,
            final Date startAt,
            final Map<String, Object> jobMap)
            throws SchedulerException {

        if (isRunning(new JobKey(jobName, Scheduler.DEFAULT_GROUP))) {
            LOG.debug("Job {} already running, cancel", jobName);
            return;
        }

        // 0. unregister job
        unregisterJob(jobName);

        // 1. JobDetail
        JobBuilder jobDetailBuilder = JobBuilder.newJob(jobClass).
                withIdentity(jobName).
                usingJobData(new JobDataMap(jobMap));

        // 2. Trigger
        if (cronExpression == null && startAt == null) {
            // Jobs added with no trigger must be durable
            scheduler.getScheduler().addJob(jobDetailBuilder.storeDurably().build(), true);
        } else {
            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().
                    withIdentity(JobNamer.getTriggerName(jobName));

            if (cronExpression == null) {
                triggerBuilder.startAt(startAt);
            } else {
                triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression));

                if (startAt == null) {
                    triggerBuilder.startNow();
                } else {
                    triggerBuilder.startAt(startAt);
                }
            }

            scheduler.getScheduler().scheduleJob(jobDetailBuilder.build(), triggerBuilder.build());
        }
    }

    @Override
    public Map<String, Object> register(
            final SchedTask task,
            final OffsetDateTime startAt,
            final long interruptMaxRetries,
            final String executor)
            throws SchedulerException {

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

        Map<String, Object> jobMap = createJobMapForExecutionContext(executor);
        jobMap.put(JobManager.TASK_KEY, task.getKey());
        jobMap.put(TaskJob.DELEGATE_IMPLEMENTATION, jobDelegate.getKey());

        registerJob(
                JobNamer.getJobKey(task).getName(),
                TaskJob.class,
                task.getCronExpression(),
                Optional.ofNullable(startAt).map(s -> new Date(s.toInstant().toEpochMilli())).orElse(null),
                jobMap);
        return jobMap;
    }

    @Override
    public void register(
            final Report report,
            final OffsetDateTime startAt,
            final long interruptMaxRetries,
            final String executor) throws SchedulerException {

        Map<String, Object> jobMap = createJobMapForExecutionContext(executor);
        jobMap.put(JobManager.REPORT_KEY, report.getKey());

        registerJob(
                JobNamer.getJobKey(report).getName(),
                ReportJob.class,
                report.getCronExpression(),
                Optional.ofNullable(startAt).map(s -> new Date(s.toInstant().toEpochMilli())).orElse(null),
                jobMap);
    }

    protected static Map<String, Object> createJobMapForExecutionContext(final String executor) {
        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put(JobManager.DOMAIN_KEY, AuthContextUtils.getDomain());
        jobMap.put(JobManager.EXECUTOR_KEY, executor);
        return jobMap;
    }

    protected void unregisterJob(final String jobName) {
        try {
            scheduler.getScheduler().unscheduleJob(new TriggerKey(jobName, Scheduler.DEFAULT_GROUP));
            scheduler.getScheduler().deleteJob(new JobKey(jobName, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            LOG.error("Could not remove job " + jobName, e);
        }
    }

    @Override
    public void unregister(final Task task) {
        unregisterJob(JobNamer.getJobKey(task).getName());
    }

    @Override
    public void unregister(final Report report) {
        unregisterJob(JobNamer.getJobKey(report).getName());
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Transactional
    @Override
    public void load(final String domain, final DataSource datasource) {
        if (disableQuartzInstance) {
            String instanceId = "AUTO";
            try {
                instanceId = scheduler.getScheduler().getSchedulerInstanceId();
                scheduler.getScheduler().standby();

                LOG.info("Successfully put Quartz instance {} in standby", instanceId);
            } catch (SchedulerException e) {
                LOG.error("Could not put Quartz instance {} in standby", instanceId, e);
            }

            return;
        }

        Pair<String, Long> conf = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            String notificationJobCronExpression = StringUtils.EMPTY;

            String notificationJobCronExp = confParamOps.get(
                    SyncopeConstants.MASTER_DOMAIN, "notificationjob.cronExpression", null, String.class);
            if (notificationJobCronExp == null) {
                notificationJobCronExpression = NotificationJob.DEFAULT_CRON_EXP;
            } else if (!StringUtils.EMPTY.equals(notificationJobCronExp)) {
                notificationJobCronExpression = notificationJobCronExp;
            }

            long interruptMaxRetries = confParamOps.get(
                    SyncopeConstants.MASTER_DOMAIN, "tasks.interruptMaxRetries", 1L, Long.class);

            return Pair.of(notificationJobCronExpression, interruptMaxRetries);
        });

        AuthContextUtils.callAsAdmin(domain, () -> {
            // 1. jobs for SchedTasks
            Set<SchedTask> tasks = new HashSet<>(taskDAO.<SchedTask>findAll(TaskType.SCHEDULED));
            tasks.addAll(taskDAO.<PullTask>findAll(TaskType.PULL));
            tasks.addAll(taskDAO.<PushTask>findAll(TaskType.PUSH));

            boolean loadException = false;
            for (Iterator<SchedTask> it = tasks.iterator(); it.hasNext() && !loadException;) {
                SchedTask task = it.next();
                try {
                    register(task, task.getStartAt(), conf.getRight(), securityProperties.getAdminUser());
                } catch (Exception e) {
                    LOG.error("While loading job instance for task " + task.getKey(), e);
                    loadException = true;
                }
            }

            if (loadException) {
                LOG.debug("Errors while loading job instances for tasks, aborting");
            } else {
                // 2. jobs for Reports
                for (Iterator<Report> it = reportDAO.findAll().iterator(); it.hasNext() && !loadException;) {
                    Report report = it.next();
                    try {
                        register(report, null, conf.getRight(), securityProperties.getAdminUser());
                    } catch (Exception e) {
                        LOG.error("While loading job instance for report " + report.getName(), e);
                        loadException = true;
                    }
                }

                if (loadException) {
                    LOG.debug("Errors while loading job instances for reports, aborting");
                }
            }

            return null;
        });

        if (SyncopeConstants.MASTER_DOMAIN.equals(domain)) {
            // 3. NotificationJob
            if (StringUtils.isBlank(conf.getLeft())) {
                LOG.debug("Empty value provided for {}'s cron, not registering anything on Quartz",
                        NotificationJob.class.getSimpleName());
            } else {
                LOG.debug("{}'s cron expression: {} - registering Quartz job and trigger",
                        NotificationJob.class.getSimpleName(), conf.getLeft());

                try {
                    Map<String, Object> jobData = createJobMapForExecutionContext(securityProperties.getAdminUser());
                    registerJob(
                            NOTIFICATION_JOB.getName(),
                            NotificationJob.class,
                            conf.getLeft(),
                            null,
                            jobData);
                } catch (Exception e) {
                    LOG.error("While loading {} instance", NotificationJob.class.getSimpleName(), e);
                }
            }

            // 4. SystemLoadReporterJob (fixed schedule, every minute)
            LOG.debug("Registering {}", SystemLoadReporterJob.class);
            try {
                Map<String, Object> jobData = createJobMapForExecutionContext(securityProperties.getAdminUser());
                registerJob(
                        StringUtils.uncapitalize(SystemLoadReporterJob.class.getSimpleName()),
                        SystemLoadReporterJob.class,
                        "0 * * * * ?",
                        null,
                        jobData);
            } catch (Exception e) {
                LOG.error("While loading {} instance", SystemLoadReporterJob.class.getSimpleName(), e);
            }
        }
    }

    @Override
    public void unload(final String domain) {
        AuthContextUtils.callAsAdmin(domain, () -> {
            // 1. jobs for SchedTasks
            Set<SchedTask> tasks = new HashSet<>(taskDAO.<SchedTask>findAll(TaskType.SCHEDULED));
            tasks.addAll(taskDAO.<PullTask>findAll(TaskType.PULL));
            tasks.addAll(taskDAO.<PushTask>findAll(TaskType.PUSH));

            tasks.forEach(task -> {
                try {
                    unregister(task);
                } catch (Exception e) {
                    LOG.error("While unloading job instance for task " + task.getKey(), e);
                }
            });

            // 2. jobs for Reports
            reportDAO.findAll().forEach(report -> {
                try {
                    unregister(report);
                } catch (Exception e) {
                    LOG.error("While unloading job instance for report " + report.getName(), e);
                }
            });

            return null;
        });
    }
}
