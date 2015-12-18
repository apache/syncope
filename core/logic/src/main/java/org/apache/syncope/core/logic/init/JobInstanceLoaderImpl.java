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
package org.apache.syncope.core.logic.init;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.provisioning.api.job.JobInstanceLoader;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.logic.notification.NotificationJob;
import org.apache.syncope.core.logic.report.ReportJob;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.provisioning.java.sync.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.sync.SyncJobDelegate;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobInstanceLoaderImpl implements JobInstanceLoader, SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JobInstanceLoader.class);

    @Autowired
    private DomainsHolder domainsHolder;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ConfDAO confDAO;

    private void registerJob(
            final String jobName, final Job jobInstance,
            final String cronExpression, final Date startAt,
            final Map<String, Object> jobMap)
            throws SchedulerException, ParseException {

        synchronized (scheduler.getScheduler()) {
            boolean jobAlreadyRunning = false;
            for (JobExecutionContext jobCtx : scheduler.getScheduler().getCurrentlyExecutingJobs()) {
                if (jobName.equals(jobCtx.getJobDetail().getKey().getName())
                        && Scheduler.DEFAULT_GROUP.equals(jobCtx.getJobDetail().getKey().getGroup())) {

                    jobAlreadyRunning = true;

                    LOG.debug("Job {} already running, cancel", jobCtx.getJobDetail().getKey());
                }
            }

            if (jobAlreadyRunning) {
                return;
            }
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
    private <T> T createSpringBean(final Class<T> jobClass) {
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
    public Map<String, Object> registerJob(final SchedTask task, final Date startAt, final long interruptMaxRetries)
            throws SchedulerException, ParseException {

        TaskJob job = createSpringBean(TaskJob.class);
        job.setTaskKey(task.getKey());

        String jobDelegateClassName = task instanceof SyncTask
                ? SyncJobDelegate.class.getName()
                : task instanceof PushTask
                        ? PushJobDelegate.class.getName()
                        : task.getJobDelegateClassName();

        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put(JobInstanceLoader.DOMAIN, AuthContextUtils.getDomain());
        jobMap.put(TaskJob.DELEGATE_CLASS_KEY, jobDelegateClassName);
        jobMap.put(TaskJob.INTERRUPT_MAX_RETRIES_KEY, interruptMaxRetries);

        registerJob(
                JobNamer.getJobName(task),
                job,
                task.getCronExpression(),
                startAt,
                jobMap);
        return jobMap;
    }

    @Override
    public void registerJob(final Report report, final Date startAt) throws SchedulerException, ParseException {
        ReportJob job = createSpringBean(ReportJob.class);
        job.setReportKey(report.getKey());

        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put(JobInstanceLoader.DOMAIN, AuthContextUtils.getDomain());

        registerJob(JobNamer.getJobName(report), job, report.getCronExpression(), startAt, jobMap);
    }

    private void registerNotificationJob(final String cronExpression) throws SchedulerException, ParseException {
        NotificationJob job = createSpringBean(NotificationJob.class);

        registerJob("taskNotificationJob", job, cronExpression, null, Collections.<String, Object>emptyMap());
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
    public void unregisterJob(final Task task) {
        unregisterJob(JobNamer.getJobName(task));
    }

    @Override
    public void unregisterJob(final Report report) {
        unregisterJob(JobNamer.getJobName(report));
    }

    @Override
    public Integer getPriority() {
        return 200;
    }

    @Transactional
    @Override
    public void load() {
        final Pair<String, Long> notificationConf = AuthContextUtils.execWithAuthContext(SyncopeConstants.MASTER_DOMAIN,
                new AuthContextUtils.Executable<Pair<String, Long>>() {

            @Override
            public Pair<String, Long> exec() {
                String notificationJobCronExpression = StringUtils.EMPTY;

                CPlainAttr notificationJobCronExp =
                        confDAO.find("notificationjob.cronExpression", NotificationJob.DEFAULT_CRON_EXP);
                if (!notificationJobCronExp.getValuesAsStrings().isEmpty()) {
                    notificationJobCronExpression = notificationJobCronExp.getValuesAsStrings().get(0);
                }

                long interruptMaxRetries = confDAO.find("tasks.interruptMaxRetries", "1").getValues().get(0).
                        getLongValue();

                return ImmutablePair.of(notificationJobCronExpression, interruptMaxRetries);
            }
        });

        for (String domain : domainsHolder.getDomains().keySet()) {
            AuthContextUtils.execWithAuthContext(domain, new AuthContextUtils.Executable<Void>() {

                @Override
                public Void exec() {
                    // 1. jobs for SchedTasks
                    Set<SchedTask> tasks = new HashSet<>(taskDAO.<SchedTask>findAll(TaskType.SCHEDULED));
                    tasks.addAll(taskDAO.<SyncTask>findAll(TaskType.SYNCHRONIZATION));
                    tasks.addAll(taskDAO.<PushTask>findAll(TaskType.PUSH));
                    for (SchedTask task : tasks) {
                        try {
                            registerJob(task, task.getStartAt(), notificationConf.getRight());
                        } catch (Exception e) {
                            LOG.error("While loading job instance for task " + task.getKey(), e);
                        }
                    }

                    // 2. ReportJobs
                    for (Report report : reportDAO.findAll()) {
                        try {
                            registerJob(report, null);
                        } catch (Exception e) {
                            LOG.error("While loading job instance for report " + report.getName(), e);
                        }
                    }

                    return null;
                }
            });
        }

        // 3. NotificationJob
        if (StringUtils.isBlank(notificationConf.getLeft())) {
            LOG.debug("Empty value provided for NotificationJob's cron, not registering anything on Quartz");
        } else {
            LOG.debug("NotificationJob's cron expression: {} - registering Quartz job and trigger",
                    notificationConf.getLeft());

            try {
                registerNotificationJob(notificationConf.getLeft());
            } catch (Exception e) {
                LOG.error("While loading NotificationJob instance", e);
            }
        }
    }
}
