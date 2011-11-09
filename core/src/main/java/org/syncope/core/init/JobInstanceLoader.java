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
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.scheduling.AppContextMethodInvokingJobDetailFactoryBean;
import org.syncope.core.scheduling.Job;
import org.syncope.core.scheduling.NotificationJob;
import org.syncope.core.util.ApplicationContextManager;

@Component
public class JobInstanceLoader extends AbstractLoader {

    private static final Logger LOG = LoggerFactory.getLogger(
            JobInstanceLoader.class);

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private TaskDAO taskDAO;

    public static String getJobName(final Long taskId) {
        return "job" + taskId;
    }

    public static String getJobProxyName(final Long taskId) {
        return "jobProxy" + taskId;
    }

    public static String getJobDetailName(final Long taskId) {
        return "jobDetail" + taskId;
    }

    public static String getTriggerName(final Long taskId) {
        return "Trigger_" + getJobDetailName(taskId);
    }

    public void registerJob(final Long taskId, final String jobClassName,
            final String cronExpression)
            throws Exception {

        unregisterJob(taskId);

        ConfigurableApplicationContext ctx =
                ApplicationContextManager.getApplicationContext();

        MutablePropertyValues mpv = new MutablePropertyValues();
        if (!NotificationJob.class.getName().equals(jobClassName)) {
            mpv.add("taskId", taskId);
        }
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClassName(jobClassName);
        bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        bd.setPropertyValues(mpv);
        getBeanFactory().registerBeanDefinition(getJobName(taskId), bd);

        mpv = new MutablePropertyValues();
        mpv.add("target", ctx.getBean(getJobName(taskId)));
        mpv.add("proxyInterfaces", Job.class.getName());
        mpv.add("interceptorNames", "jpaInterceptor");
        bd = new GenericBeanDefinition();
        bd.setBeanClass(ProxyFactoryBean.class);
        bd.setPropertyValues(mpv);
        getBeanFactory().registerBeanDefinition(
                getJobProxyName(taskId), bd);

        AppContextMethodInvokingJobDetailFactoryBean jobDetailFactory =
                (AppContextMethodInvokingJobDetailFactoryBean) getBeanFactory().
                autowire(AppContextMethodInvokingJobDetailFactoryBean.class,
                AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
        jobDetailFactory.setTargetBeanName(getJobProxyName(taskId));
        jobDetailFactory.setTargetMethod("execute");
        jobDetailFactory.afterPropertiesSet();
        getBeanFactory().registerSingleton(getJobDetailName(taskId),
                jobDetailFactory);

        JobDetail jobDetail = (JobDetail) ctx.getBean(
                getJobDetailName(taskId));
        jobDetail.setName(getJobDetailName(taskId));
        jobDetail.setGroup(Scheduler.DEFAULT_GROUP);

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
                    getJobDetailName(taskId), Scheduler.DEFAULT_GROUP);
            scheduler.getScheduler().deleteJob(
                    getJobDetailName(taskId), Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.error("Could not remove job " + getJobDetailName(taskId), e);
        }

        if (getBeanFactory().containsSingleton(getJobDetailName(taskId))) {
            getBeanFactory().destroySingleton(getJobDetailName(taskId));
        }
        if (getBeanFactory().containsBeanDefinition(getJobProxyName(taskId))) {

            getBeanFactory().removeBeanDefinition(getJobProxyName(taskId));
        }
        if (getBeanFactory().containsBeanDefinition(getJobName(taskId))) {
            getBeanFactory().removeBeanDefinition(getJobName(taskId));
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
