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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.identityconnectors.common.IOUtil;
import org.quartz.impl.jdbcjobstore.Constants;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;

public class JobManagerImpl implements JobManager, SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JobManager.class);

    @Autowired
    private DomainHolder domainHolder;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private ConfParamOps confParamOps;

    @Resource(name = "adminUser")
    private String adminUser;

    private boolean disableQuartzInstance;

    public void setDisableQuartzInstance(final boolean disableQuartzInstance) {
        this.disableQuartzInstance = disableQuartzInstance;
    }

    private boolean isRunningHere(final JobKey jobKey) throws SchedulerException {
        return scheduler.getScheduler().getCurrentlyExecutingJobs().stream().
                anyMatch(jec -> jobKey.equals(jec.getJobDetail().getKey()));
    }

    private boolean isRunningElsewhere(final JobKey jobKey) throws SchedulerException {
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

    private void registerJob(
            final String jobName, final Job jobInstance,
            final String cronExpression, final Date startAt,
            final Map<String, Object> jobMap)
            throws SchedulerException {

        if (isRunning(new JobKey(jobName, Scheduler.DEFAULT_GROUP))) {
            LOG.debug("Job {} already running, cancel", jobName);
            return;
        }

        // 0. unregister job
        unregisterJob(jobName);

        // 1. Job bean
        ApplicationContextProvider.getBeanFactory().registerSingleton(jobName, jobInstance);

        // 2. JobDetail bean
        JobBuilder jobDetailBuilder = JobBuilder.newJob(jobInstance.getClass()).
                withIdentity(jobName).
                usingJobData(new JobDataMap(jobMap));

        // 3. Trigger
        if (cronExpression == null && startAt == null) {
            // Jobs added with no trigger must be durable
            scheduler.getScheduler().addJob(jobDetailBuilder.storeDurably().build(), true);
        } else {
            TriggerBuilder<?> triggerBuilder;

            if (cronExpression == null) {
                triggerBuilder = TriggerBuilder.newTrigger().
                        withIdentity(JobNamer.getTriggerName(jobName)).
                        startAt(startAt);
            } else {
                triggerBuilder = TriggerBuilder.newTrigger().
                        withIdentity(JobNamer.getTriggerName(jobName)).
                        withSchedule(CronScheduleBuilder.cronSchedule(cronExpression));

                if (startAt == null) {
                    triggerBuilder = triggerBuilder.startNow();
                } else {
                    triggerBuilder = triggerBuilder.startAt(startAt);
                }
            }

            scheduler.getScheduler().scheduleJob(jobDetailBuilder.build(), triggerBuilder.build());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createSpringBean(final Class<T> jobClass) {
        T jobInstance = null;
        for (int i = 0; i < 5 && jobInstance == null; i++) {
            LOG.debug("{} attempt to create Spring bean for {}", i, jobClass);
            try {
                jobInstance = (T) ApplicationContextProvider.getBeanFactory().
                        createBean(jobClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                LOG.debug("{} attempt to create Spring bean for {} succeeded", i, jobClass);
            } catch (BeanCreationException e) {
                LOG.error("Could not create Spring bean for {}", jobClass, e);
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException ex) {
                    // ignore
                }
            }
        }
        if (jobInstance == null) {
            throw new NotFoundException("Spring bean for " + jobClass);
        }

        return jobInstance;
    }

    @Override
    public Map<String, Object> register(final SchedTask task, final Date startAt, final long interruptMaxRetries,
            final String executor)
            throws SchedulerException {

        TaskJob job = createSpringBean(TaskJob.class);
        job.setTaskKey(task.getKey());

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
        jobMap.put(TaskJob.DELEGATE_IMPLEMENTATION, jobDelegate.getKey());

        registerJob(
                JobNamer.getJobKey(task).getName(),
                job,
                task.getCronExpression(),
                startAt,
                jobMap);
        return jobMap;
    }

    @Override
    public void register(final Report report, final Date startAt, final long interruptMaxRetries,
            final String executor) throws SchedulerException {

        ReportJob job = createSpringBean(ReportJob.class);
        job.setReportKey(report.getKey());

        Map<String, Object> jobMap = createJobMapForExecutionContext(executor);

        registerJob(JobNamer.getJobKey(report).getName(), job, report.getCronExpression(), startAt, jobMap);
    }

    private static Map<String, Object> createJobMapForExecutionContext(final String executor) {
        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put(JobManager.DOMAIN_KEY, AuthContextUtils.getDomain());
        jobMap.put(JobManager.EXECUTOR_KEY, executor);
        return jobMap;
    }

    private void unregisterJob(final String jobName) {
        try {
            scheduler.getScheduler().unscheduleJob(new TriggerKey(jobName, Scheduler.DEFAULT_GROUP));
            scheduler.getScheduler().deleteJob(new JobKey(jobName, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            LOG.error("Could not remove job " + jobName, e);
        }

        if (ApplicationContextProvider.getBeanFactory().containsSingleton(jobName)) {
            ApplicationContextProvider.getBeanFactory().destroySingleton(jobName);
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
                    register(task, task.getStartAt(), conf.getRight(), adminUser);
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
                        register(report, null, conf.getRight(), adminUser);
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
                    NotificationJob job = createSpringBean(NotificationJob.class);
                    Map<String, Object> jobData = createJobMapForExecutionContext(adminUser);
                    registerJob(
                            NOTIFICATION_JOB.getName(),
                            job,
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
                SystemLoadReporterJob job = createSpringBean(SystemLoadReporterJob.class);
                Map<String, Object> jobData = createJobMapForExecutionContext(adminUser);
                registerJob(
                        StringUtils.uncapitalize(SystemLoadReporterJob.class.getSimpleName()),
                        job,
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
