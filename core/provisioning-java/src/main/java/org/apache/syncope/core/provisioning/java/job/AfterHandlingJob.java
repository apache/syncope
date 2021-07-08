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

import java.util.Map;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Quartz job for asynchronous handling of notification / audit events.
 * Instead of direct synchronous invocation - which occurs in the same transaction where the event is generated, the
 * execution of the scheduled code happens in a new transaction.
 */
public class AfterHandlingJob extends AbstractInterruptableJob {

    private static final Logger LOG = LoggerFactory.getLogger(AfterHandlingJob.class);

    public static void schedule(final SchedulerFactoryBean scheduler, final Map<String, Object> jobMap) {
        @SuppressWarnings("unchecked")
        AfterHandlingJob jobInstance = (AfterHandlingJob) ApplicationContextProvider.getBeanFactory().
                createBean(AfterHandlingJob.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        String jobName = AfterHandlingJob.class.getSimpleName() + SecureRandomUtils.generateRandomUUID();

        jobMap.put(JobManager.DOMAIN_KEY, AuthContextUtils.getDomain());

        ApplicationContextProvider.getBeanFactory().registerSingleton(jobName, jobInstance);

        JobBuilder jobDetailBuilder = JobBuilder.newJob(AfterHandlingJob.class).
                withIdentity(jobName, Scheduler.DEFAULT_GROUP).
                usingJobData(new JobDataMap(jobMap));

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().
                withIdentity(JobNamer.getTriggerName(jobName), Scheduler.DEFAULT_GROUP).
                startNow();

        try {
            scheduler.getScheduler().scheduleJob(jobDetailBuilder.build(), triggerBuilder.build());
        } catch (SchedulerException e) {
            LOG.error("Could not schedule, aborting", e);
        }
    }

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            AuthContextUtils.callAsAdmin(context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY),
                    () -> {
                        notificationManager.createTasks(
                                (AfterHandlingEvent) context.getMergedJobDataMap().get(AfterHandlingEvent.JOBMAP_KEY));
                        auditManager.audit(
                                (AfterHandlingEvent) context.getMergedJobDataMap().get(AfterHandlingEvent.JOBMAP_KEY));
                        return null;
                    });
        } catch (RuntimeException e) {
            throw new JobExecutionException("While handling notification / audit events", e);
        } finally {
            ApplicationContextProvider.getBeanFactory().destroySingleton(context.getJobDetail().getKey().getName());
        }
    }
}
