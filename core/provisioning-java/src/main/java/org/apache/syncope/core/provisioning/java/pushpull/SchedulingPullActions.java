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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.Map;
import java.util.UUID;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Superclass for pull actions that need to schedule actions to run after their completion.
 *
 * @see LDAPMembershipPullActions for a concrete example
 */
public abstract class SchedulingPullActions extends DefaultPullActions {

    @Autowired
    protected SchedulerFactoryBean scheduler;

    protected <T extends Job> void schedule(final Class<T> reference, final Map<String, Object> jobMap)
            throws JobExecutionException {

        @SuppressWarnings("unchecked")
        T jobInstance = (T) ApplicationContextProvider.getBeanFactory().
                createBean(reference, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        String jobName = getClass().getName() + UUID.randomUUID();

        jobMap.put(JobManager.DOMAIN_KEY, AuthContextUtils.getDomain());
        jobMap.put(JobManager.INTERRUPT_MAX_RETRIES_KEY, 1L);

        ApplicationContextProvider.getBeanFactory().registerSingleton(jobName, jobInstance);

        JobBuilder jobDetailBuilder = JobBuilder.newJob(reference).
                withIdentity(jobName).
                usingJobData(new JobDataMap(jobMap));

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().
                withIdentity(JobNamer.getTriggerName(jobName)).
                startNow();

        try {
            scheduler.getScheduler().scheduleJob(jobDetailBuilder.build(), triggerBuilder.build());
        } catch (SchedulerException e) {
            throw new JobExecutionException("Could not schedule, aborting", e);
        }
    }
}
