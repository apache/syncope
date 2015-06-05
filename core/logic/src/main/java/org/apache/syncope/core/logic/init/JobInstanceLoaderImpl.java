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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.syncope.core.provisioning.api.job.SyncJob;
import org.apache.syncope.core.provisioning.api.job.TaskJob;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.logic.notification.NotificationJob;
import org.apache.syncope.core.logic.report.ReportJob;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.provisioning.api.job.PushJob;
import org.apache.syncope.core.provisioning.java.sync.PushJobImpl;
import org.apache.syncope.core.provisioning.java.sync.SyncJobImpl;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
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
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ConfDAO confDAO;

    private void registerJob(final String jobName, final Job jobInstance, final String cronExpression)
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
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName(jobName);
        jobDetail.setGroup(Scheduler.DEFAULT_GROUP);
        jobDetail.setJobClass(jobInstance.getClass());

        // 3. Trigger
        if (cronExpression == null) {
            // Jobs added with no trigger must be durable
            jobDetail.setDurability(true);
            scheduler.getScheduler().addJob(jobDetail, true);
        } else {
            CronTriggerImpl cronTrigger = new CronTriggerImpl();
            cronTrigger.setName(JobNamer.getTriggerName(jobName));
            cronTrigger.setCronExpression(cronExpression);

            scheduler.getScheduler().scheduleJob(jobDetail, cronTrigger);
        }
    }

    private Job createSpringBean(final Class<?> jobClass) {
        Job jobInstance = null;
        for (int i = 0; i < 5 && jobInstance == null; i++) {
            LOG.debug("{} attempt to create Spring bean for {}", i, jobClass);
            try {
                jobInstance = (Job) ApplicationContextProvider.getBeanFactory().
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

    @SuppressWarnings("unchecked")
    @Override
    public void registerJob(final Task task, final String jobClassName, final String cronExpression)
            throws ClassNotFoundException, SchedulerException, ParseException {

        Class<?> jobClass = Class.forName(jobClassName);
        if (SyncJob.class.equals(jobClass)) {
            jobClass = SyncJobImpl.class;
        } else if (PushJob.class.equals(jobClass)) {
            jobClass = PushJobImpl.class;
        }

        Job jobInstance = createSpringBean(jobClass);
        if (jobInstance instanceof TaskJob) {
            ((TaskJob) jobInstance).setTaskId(task.getKey());
        }

        // In case of synchronization job/task retrieve and set synchronization actions:
        // actions cannot be changed at runtime but connector and synchronization policies (reloaded at execution time).
        if (jobInstance instanceof SyncJob && task instanceof SyncTask) {
            final List<SyncActions> actions = new ArrayList<>();
            for (String className : ((SyncTask) task).getActionsClassNames()) {
                try {
                    Class<?> actionsClass = Class.forName(className);

                    SyncActions syncActions = (SyncActions) ApplicationContextProvider.getBeanFactory().
                            createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                    actions.add(syncActions);
                } catch (Exception e) {
                    LOG.info("Class '{}' not found", className, e);
                }
            }

            ((SyncJob) jobInstance).setActions(actions);
        }

        registerJob(JobNamer.getJobName(task), jobInstance, cronExpression);
    }

    @Transactional(readOnly = true)
    @Override
    public void registerTaskJob(final Long taskKey)
            throws ClassNotFoundException, SchedulerException, ParseException {

        SchedTask task = taskDAO.find(taskKey);
        if (task == null) {
            throw new NotFoundException("Task " + taskKey);
        } else {
            registerJob(task, task.getJobClassName(), task.getCronExpression());
        }
    }

    @Override
    public void registerJob(final Report report) throws SchedulerException, ParseException {
        Job jobInstance = createSpringBean(ReportJob.class);
        ((ReportJob) jobInstance).setReportKey(report.getKey());

        registerJob(JobNamer.getJobName(report), jobInstance, report.getCronExpression());
    }

    @Transactional(readOnly = true)
    @Override
    public void registerReportJob(final Long reportKey) throws SchedulerException, ParseException {
        Report report = reportDAO.find(reportKey);
        if (report == null) {
            throw new NotFoundException("Report " + reportKey);
        } else {
            registerJob(report);
        }
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
        // 1. jobs for SchedTasks
        Set<SchedTask> tasks = new HashSet<>(taskDAO.<SchedTask>findAll(TaskType.SCHEDULED));
        tasks.addAll(taskDAO.<SyncTask>findAll(TaskType.SYNCHRONIZATION));
        tasks.addAll(taskDAO.<PushTask>findAll(TaskType.PUSH));
        for (SchedTask task : tasks) {
            try {
                registerJob(task, task.getJobClassName(), task.getCronExpression());
            } catch (Exception e) {
                LOG.error("While loading job instance for task " + task.getKey(), e);
            }
        }

        // 2. NotificationJob
        CPlainAttr notificationJobCronExp =
                confDAO.find("notificationjob.cronExpression", NotificationJob.DEFAULT_CRON_EXP);
        if (StringUtils.isBlank(notificationJobCronExp.getValuesAsStrings().get(0))) {
            LOG.debug("Empty value provided for NotificationJob's cron, not registering anything on Quartz");
        } else {
            LOG.debug("NotificationJob's cron expression: {} - registering Quartz job and trigger",
                    notificationJobCronExp);

            try {
                registerJob(null, NotificationJob.class.getName(), notificationJobCronExp.getValuesAsStrings().get(0));
            } catch (Exception e) {
                LOG.error("While loading NotificationJob instance", e);
            }
        }

        // 3. ReportJobs
        for (Report report : reportDAO.findAll()) {
            try {
                registerJob(report);
            } catch (Exception e) {
                LOG.error("While loading job instance for report " + report.getName(), e);
            }
        }
    }
}
