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
package org.apache.syncope.core.init;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.notification.NotificationJob;
import org.apache.syncope.core.persistence.beans.Report;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.ReportDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.quartz.AbstractTaskJob;
import org.apache.syncope.core.report.ReportJob;
import org.apache.syncope.core.sync.DefaultSyncActions;
import org.apache.syncope.core.sync.SyncActions;
import org.apache.syncope.core.sync.impl.SyncJob;
import org.apache.syncope.core.util.ApplicationContextProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobInstanceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JobInstanceLoader.class);

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ConfDAO confDAO;

    private static Long getIdFromJobName(final String name, final String pattern, final int prefixLength) {
        Long result = null;

        Matcher jobMatcher = Pattern.compile(pattern).matcher(name);
        if (jobMatcher.matches()) {
            try {
                result = Long.valueOf(name.substring(prefixLength));
            } catch (NumberFormatException e) {
                LOG.error("Unparsable id: {}", name.substring(prefixLength), e);
            }
        }

        return result;
    }

    public static Long getTaskIdFromJobName(final String name) {
        return getIdFromJobName("taskJob[0-9]+", name, 7);
    }

    public static Long getReportIdFromJobName(final String name) {
        return getIdFromJobName("reportJob[0-9]+", name, 9);
    }

    public static String getJobName(final Task task) {
        return task == null
                ? "taskNotificationJob"
                : "taskJob" + task.getId();
    }

    public static String getJobName(final Report report) {
        return "reportJob" + report.getId();
    }

    public static String getTriggerName(final String jobName) {
        return "Trigger_" + jobName;
    }

    private void registerJob(final String jobName, final Job jobInstance, final String cronExpression)
            throws SchedulerException, ParseException {

        synchronized (scheduler.getScheduler()) {
            boolean jobAlreadyRunning = false;
            for (JobExecutionContext jobCtx : (List<JobExecutionContext>) scheduler.getScheduler().
                    getCurrentlyExecutingJobs()) {

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
            scheduler.getScheduler().addJob(jobDetail, true);
        } else {
            CronTriggerImpl cronTrigger = new CronTriggerImpl();
            cronTrigger.setName(getTriggerName(jobName));
            cronTrigger.setCronExpression(cronExpression);

            scheduler.getScheduler().scheduleJob(jobDetail, cronTrigger);
        }
    }

    public void registerJob(final Task task, final String jobClassName, final String cronExpression)
            throws ClassNotFoundException, SchedulerException, ParseException {

        Class jobClass = Class.forName(jobClassName);
        Job jobInstance = (Job) ApplicationContextProvider.getBeanFactory().
                createBean(jobClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        if (jobInstance instanceof AbstractTaskJob) {
            ((AbstractTaskJob) jobInstance).setTaskId(task.getId());
        }
        if (jobInstance instanceof SyncJob && task instanceof SyncTask) {
            String jobActionsClassName = ((SyncTask) task).getActionsClassName();
            Class syncActionsClass = DefaultSyncActions.class;
            if (StringUtils.isNotBlank(jobActionsClassName)) {
                try {
                    syncActionsClass = Class.forName(jobActionsClassName);
                } catch (Exception e) {
                    LOG.error("Class {} not found, reverting to {}", jobActionsClassName,
                            syncActionsClass.getName(), e);
                }
            }
            SyncActions syncActions = (SyncActions) ApplicationContextProvider.getBeanFactory().
                    createBean(syncActionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

            ((SyncJob) jobInstance).setActions(syncActions);
        }

        registerJob(getJobName(task), jobInstance, cronExpression);
    }

    public void registerJob(final Report report) throws SchedulerException, ParseException {
        Job jobInstance = (Job) ApplicationContextProvider.getBeanFactory().
                createBean(ReportJob.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        ((ReportJob) jobInstance).setReportId(report.getId());

        registerJob(getJobName(report), jobInstance, report.getCronExpression());
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

    public void unregisterJob(final Task task) {
        unregisterJob(getJobName(task));
    }

    public void unregisterJob(final Report report) {
        unregisterJob(getJobName(report));
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void load() {
        // 1. jobs for SchedTasks
        Set<SchedTask> tasks = new HashSet(taskDAO.findAll(SchedTask.class));
        tasks.addAll(taskDAO.findAll(SyncTask.class));
        for (SchedTask task : tasks) {
            try {
                registerJob(task, task.getJobClassName(), task.getCronExpression());
            } catch (Exception e) {
                LOG.error("While loading job instance for task " + task.getId(), e);
            }
        }

        // 2. NotificationJob
        final String notificationJobCronExp =
                confDAO.find("notificationjob.cronExpression", NotificationJob.DEFAULT_CRON_EXP).getValue();
        if (StringUtils.isBlank(notificationJobCronExp)) {
            LOG.debug("Empty value provided for NotificationJob's cron, not registering anything on Quartz");
        } else {
            LOG.debug("NotificationJob's cron expression: {} - registering Quartz job and trigger",
                    notificationJobCronExp);

            try {
                registerJob(null, NotificationJob.class.getName(), notificationJobCronExp);
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
