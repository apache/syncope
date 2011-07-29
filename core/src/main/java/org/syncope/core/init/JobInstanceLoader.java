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
import org.syncope.core.util.ApplicationContextManager;

@Component
public class JobInstanceLoader extends AbstractLoader {

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

    public void registerJob(final SchedTask task)
            throws Exception {

        unregisterJob(task.getId());

        ConfigurableApplicationContext ctx =
                ApplicationContextManager.getApplicationContext();

        MutablePropertyValues mpv = new MutablePropertyValues();
        mpv.add("taskId", task.getId());
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClassName(task.getJobClassName());
        bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        bd.setPropertyValues(mpv);
        getBeanFactory().registerBeanDefinition(getJobName(task.getId()), bd);

        mpv = new MutablePropertyValues();
        mpv.add("target", ctx.getBean(getJobName(task.getId())));
        mpv.add("proxyInterfaces", Job.class.getName());
        mpv.add("interceptorNames", "jpaInterceptor");
        bd = new GenericBeanDefinition();
        bd.setBeanClass(ProxyFactoryBean.class);
        bd.setPropertyValues(mpv);
        getBeanFactory().registerBeanDefinition(
                getJobProxyName(task.getId()), bd);

        AppContextMethodInvokingJobDetailFactoryBean jobDetailFactory =
                (AppContextMethodInvokingJobDetailFactoryBean) getBeanFactory().
                autowire(AppContextMethodInvokingJobDetailFactoryBean.class,
                AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
        jobDetailFactory.setTargetBeanName(getJobProxyName(task.getId()));
        jobDetailFactory.setTargetMethod("execute");
        jobDetailFactory.afterPropertiesSet();
        getBeanFactory().registerSingleton(getJobDetailName(task.getId()),
                jobDetailFactory);

        JobDetail jobDetail = (JobDetail) ctx.getBean(
                getJobDetailName(task.getId()));
        jobDetail.setName(getJobDetailName(task.getId()));
        jobDetail.setGroup(Scheduler.DEFAULT_GROUP);

        if (task.getCronExpression() == null) {
            scheduler.getScheduler().addJob(jobDetail, true);
        } else {
            CronTriggerBean cronTrigger = new CronTriggerBean();
            cronTrigger.setName(getTriggerName(task.getId()));
            cronTrigger.setCronExpression(task.getCronExpression());

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
        List<SchedTask> tasks = taskDAO.findAll(SchedTask.class);
        tasks.addAll(taskDAO.findAll(SyncTask.class));
        for (SchedTask task : tasks) {
            try {
                registerJob(task);
            } catch (Exception e) {
                LOG.error("While loading job instance for task "
                        + task.getId(), e);
            }
        }
    }
}
