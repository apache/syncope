/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.init;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.scheduling.quartz.JobDetailBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.dao.ReportDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.scheduling.AbstractTaskJob;
import org.syncope.core.scheduling.DefaultSyncJobActions;
import org.syncope.core.scheduling.NotificationJob;
import org.syncope.core.scheduling.ReportJob;
import org.syncope.core.scheduling.SyncJob;
import org.syncope.core.scheduling.SyncJobActions;

@Component
public class JobInstanceLoader extends AbstractLoader {

    private static final Logger LOG = LoggerFactory.getLogger(
            JobInstanceLoader.class);

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ReportDAO reportDAO;

    private static Long getIdFromJobName(final String name,
            final String pattern, final int prefixLength) {

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

    private void registerJob(final String jobName,
            final Job jobInstance, final String cronExpression)
            throws Exception {

        // 0. unregister job
        unregisterJob(jobName);

        // 1. Job bean
        getBeanFactory().registerSingleton(jobName, jobInstance);

        // 2. JobDetail bean
        JobDetail jobDetail = new JobDetailBean();
        jobDetail.setName(jobName);
        jobDetail.setGroup(Scheduler.DEFAULT_GROUP);
        jobDetail.setJobClass(jobInstance.getClass());

        // 3. Trigger
        if (cronExpression == null) {
            scheduler.getScheduler().addJob(jobDetail, true);
        } else {
            CronTriggerBean cronTrigger = new CronTriggerBean();
            cronTrigger.setName(getTriggerName(jobName));
            cronTrigger.setCronExpression(cronExpression);

            scheduler.getScheduler().scheduleJob(jobDetail, cronTrigger);
        }
    }

    public void registerJob(final Task task, final String jobClassName,
            final String cronExpression)
            throws Exception {

        Class jobClass = Class.forName(jobClassName);
        Job jobInstance = (Job) getBeanFactory().autowire(jobClass,
                AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        if (jobInstance instanceof AbstractTaskJob) {
            ((AbstractTaskJob) jobInstance).setTaskId(task.getId());
        }
        if (jobInstance instanceof SyncJob) {
            String jobActionsClassName =
                    ((SyncTask) task).getJobActionsClassName();
            Class syncJobActionsClass = DefaultSyncJobActions.class;
            if (StringUtils.isNotBlank(jobActionsClassName)) {
                try {
                    syncJobActionsClass = Class.forName(jobActionsClassName);
                } catch (Throwable t) {
                    LOG.error("Class {} not found, reverting to {}",
                            new Object[]{jobActionsClassName,
                                syncJobActionsClass.getName(), t});
                }
            }
            SyncJobActions syncJobActions =
                    (SyncJobActions) getBeanFactory().autowire(
                    syncJobActionsClass,
                    AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

            ((SyncJob) jobInstance).setActions(syncJobActions);
        }

        registerJob(getJobName(task), jobInstance, cronExpression);
    }

    public void registerJob(final Report report)
            throws Exception {

        Job jobInstance = (Job) getBeanFactory().autowire(ReportJob.class,
                AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        ((ReportJob) jobInstance).setReportId(report.getId());

        registerJob(getJobName(report), jobInstance,
                report.getCronExpression());
    }

    private void unregisterJob(final String jobName) {
        try {
            scheduler.getScheduler().unscheduleJob(
                    jobName, Scheduler.DEFAULT_GROUP);
            scheduler.getScheduler().deleteJob(
                    jobName, Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.error("Could not remove job " + jobName, e);
        }

        if (getBeanFactory().containsSingleton(jobName)) {
            getBeanFactory().destroySingleton(jobName);
        }
    }

    public void unregisterJob(final Task task) {
        unregisterJob(getJobName(task));
    }

    public void unregisterJob(final Report report) {
        unregisterJob(getJobName(report));
    }

    @Override
    @Transactional(readOnly = true)
    public void load() {
        // 1. jobs for SchedTasks
        Set<SchedTask> tasks = new HashSet(taskDAO.findAll(SchedTask.class));
        tasks.addAll(taskDAO.findAll(SyncTask.class));
        for (SchedTask task : tasks) {
            try {
                registerJob(task, task.getJobClassName(),
                        task.getCronExpression());
            } catch (Exception e) {
                LOG.error("While loading job instance for task "
                        + task.getId(), e);
            }
        }

        // 2. NotificationJob
        try {
            registerJob(null, NotificationJob.class.getName(),
                    "0 0/2 * * * ?");
        } catch (Exception e) {
            LOG.error("While loading NotificationJob instance", e);
        }

        // 3. ReportJobs
        for (Report report : reportDAO.findAll()) {
            try {
                registerJob(report);
            } catch (Exception e) {
                LOG.error("While loading job instance for report "
                        + report.getName(), e);
            }
        }
    }
}
