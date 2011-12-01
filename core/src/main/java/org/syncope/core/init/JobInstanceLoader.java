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

import java.util.List;
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
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.scheduling.AbstractJob;
import org.syncope.core.scheduling.DefaultSyncJobActions;
import org.syncope.core.scheduling.NotificationJob;
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

    public static Long getTaskIdFromJobName(final String name) {
        Long result = null;

        Matcher jobMatcher = Pattern.compile("job[0-9]+").matcher(name);
        if (jobMatcher.matches()) {
            try {
                result = Long.valueOf(name.substring(3));
            } catch (NumberFormatException e) {
                LOG.error("Unparsable task id: {}", name.substring(3), e);
            }
        }

        return result;
    }

    public static String getJobName(final Long taskId) {
        return "job" + taskId;
    }

    public static String getTriggerName(final Long taskId) {
        return "Trigger_" + getJobName(taskId);
    }

    public void registerJob(final Long taskId, final String jobClassName,
            final String cronExpression)
            throws Exception {

        // 0. unregister job
        unregisterJob(taskId);

        // 1. Job bean
        Class jobClass = Class.forName(jobClassName);
        Job jobInstance = (Job) getBeanFactory().autowire(jobClass,
                AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        if (jobInstance instanceof AbstractJob) {
            ((AbstractJob) jobInstance).setTaskId(taskId);
        }
        if (jobInstance instanceof SyncJob) {
            String jobActionsClassName =
                    ((SyncTask) taskDAO.find(taskId)).getJobActionsClassName();
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
        getBeanFactory().registerSingleton(getJobName(taskId), jobInstance);

        // 2. JobDetail bean
        JobDetail jobDetail = new JobDetailBean();
        jobDetail.setName(getJobName(taskId));
        jobDetail.setGroup(Scheduler.DEFAULT_GROUP);
        jobDetail.setJobClass(jobClass);

        // 3. Trigger
        if (cronExpression == null) {
            scheduler.getScheduler().addJob(jobDetail, true);
        } else {
            CronTriggerBean cronTrigger = new CronTriggerBean();
            cronTrigger.setName(getTriggerName(taskId));
            cronTrigger.setCronExpression(cronExpression);

            scheduler.getScheduler().scheduleJob(jobDetail, cronTrigger);
        }
    }

    public void unregisterJob(final Long taskId) {
        try {
            scheduler.getScheduler().unscheduleJob(
                    getJobName(taskId), Scheduler.DEFAULT_GROUP);
            scheduler.getScheduler().deleteJob(
                    getJobName(taskId), Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.error("Could not remove job " + getJobName(taskId), e);
        }

        if (getBeanFactory().containsSingleton(getJobName(taskId))) {
            getBeanFactory().destroySingleton(getJobName(taskId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void load() {
        // 1. jobs for SchedTasks
        List<SchedTask> tasks = taskDAO.findAll(SchedTask.class);
        tasks.addAll(taskDAO.findAll(SyncTask.class));
        for (SchedTask task : tasks) {
            try {
                registerJob(task.getId(), task.getJobClassName(),
                        task.getCronExpression());
            } catch (Exception e) {
                LOG.error("While loading job instance for task "
                        + task.getId(), e);
            }
        }

        // 2.NotificationJob
        try {
            registerJob(-1L, NotificationJob.class.getName(),
                    "0 0/2 * * * ?");
        } catch (Exception e) {
            LOG.error("While loading NotificationJob instance", e);
        }
    }
}
