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
package org.syncope.core.scheduling;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.quartz.JobMethodInvocationFailedException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.MethodInvoker;
import org.syncope.core.util.ApplicationContextManager;

/**
 * Inspired by Spring's MethodInvokingJobDetailFactoryBean: the main difference
 * is about the MethodInvoker instance that is created at each execution of the
 * JobDetail.
 *
 * @see org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean
 */
public class AppContextMethodInvokingJobDetailFactoryBean
        implements FactoryBean<JobDetail>, BeanNameAware, BeanClassLoaderAware,
        BeanFactoryAware, InitializingBean, Serializable {

    private static final long serialVersionUID = -9164669094205867738L;

    private String group = Scheduler.DEFAULT_GROUP;

    private String beanName;

    private String[] jobListenerNames;

    private JobDetail jobDetail;

    private String targetBeanName;

    private String targetMethod;

    /**
     * Set the group of the job.
     * <p>Default is the default group of the Scheduler.
     * @see org.quartz.JobDetail#setGroup
     * @see org.quartz.Scheduler#DEFAULT_GROUP
     */
    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public JobDetail getObject()
            throws Exception {

        return jobDetail;
    }

    @Override
    public Class<?> getObjectType() {
        return JobDetail.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Set a list of JobListener names for this job, referring to
     * non-global JobListeners registered with the Scheduler.
     * <p>A JobListener name always refers to the name returned
     * by the JobListener implementation.
     * @see SchedulerFactoryBean#setJobListeners
     * @see org.quartz.JobListener#getName
     */
    public void setJobListenerNames(String[] names) {
        if (names != null) {
            this.jobListenerNames = names.clone();
        }
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
    }

    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = targetBeanName;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    @Override
    public void afterPropertiesSet()
            throws Exception {

        if (StringUtils.isBlank(targetBeanName)
                || StringUtils.isBlank(targetMethod)) {

            throw new IllegalArgumentException(
                    "Blank targetBeanName and / or targetMethod");
        }

        String name = this.beanName + "_"
                + Calendar.getInstance().getTimeInMillis();

        // Build JobDetail instance.
        this.jobDetail = new JobDetail(name, this.group,
                MethodInvokingJob.class);
        this.jobDetail.getJobDataMap().put("targetBeanName", targetBeanName);
        this.jobDetail.getJobDataMap().put("targetMethod", targetMethod);
        this.jobDetail.setVolatility(false);
        this.jobDetail.setDurability(true);

        // Register job listener names.
        if (this.jobListenerNames != null) {
            for (String jobListenerName : this.jobListenerNames) {
                this.jobDetail.addJobListener(jobListenerName);
            }
        }
    }

    public static class MethodInvokingJob extends QuartzJobBean {

        private String targetBeanName;

        private String targetMethod;

        public void setTargetBeanName(String targetBeanName) {
            this.targetBeanName = targetBeanName;
        }

        public void setTargetMethod(String targetMethod) {
            this.targetMethod = targetMethod;
        }

        @Override
        protected void executeInternal(JobExecutionContext context)
                throws JobExecutionException {

            ConfigurableApplicationContext appContext =
                    ApplicationContextManager.getApplicationContext();
            Object instance = appContext.getBean(targetBeanName);

            MethodInvoker methodInvoker = new MethodInvoker();
            methodInvoker.setTargetObject(instance);
            methodInvoker.setTargetMethod(targetMethod);

            try {
                methodInvoker.prepare();
                context.setResult(methodInvoker.invoke());
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof JobExecutionException) {
                    // JobExecutionException, to be logged by Quartz
                    throw (JobExecutionException) e.getTargetException();
                } else {
                    // "unhandled exception", to be logged by Quartz
                    throw new JobMethodInvocationFailedException(methodInvoker,
                            e.getTargetException());
                }
            } catch (Exception e) {
                // "unhandled exception", to be logged at error level by Quartz
                throw new JobMethodInvocationFailedException(methodInvoker, e);
            }
        }
    }
}
