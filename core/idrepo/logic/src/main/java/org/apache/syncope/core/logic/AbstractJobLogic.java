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
package org.apache.syncope.core.logic;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.provisioning.java.job.AbstractInterruptableJob;
import org.apache.syncope.core.provisioning.java.job.SystemLoadReporterJob;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

abstract class AbstractJobLogic<T extends EntityTO> extends AbstractTransactionalLogic<T> {

    protected final JobManager jobManager;

    protected final SchedulerFactoryBean scheduler;

    protected AbstractJobLogic(final JobManager jobManager, final SchedulerFactoryBean scheduler) {
        this.jobManager = jobManager;
        this.scheduler = scheduler;
    }

    protected abstract Triple<JobType, String, String> getReference(JobKey jobKey);

    protected JobTO getJobTO(final JobKey jobKey, final boolean includeCustom) throws SchedulerException {
        JobTO jobTO = null;

        if (scheduler.getScheduler().checkExists(jobKey)) {
            Triple<JobType, String, String> reference = getReference(jobKey);
            if (reference != null) {
                jobTO = new JobTO();
                jobTO.setType(reference.getLeft());
                jobTO.setRefKey(reference.getMiddle());
                jobTO.setRefDesc(reference.getRight());
            } else if (includeCustom) {
                JobDetail jobDetail = scheduler.getScheduler().getJobDetail(jobKey);
                if (!TaskJob.class.isAssignableFrom(jobDetail.getJobClass())
                        && !ReportJob.class.isAssignableFrom(jobDetail.getJobClass())
                        && !SystemLoadReporterJob.class.isAssignableFrom(jobDetail.getJobClass())
                        && !NotificationJob.class.isAssignableFrom(jobDetail.getJobClass())) {

                    jobTO = new JobTO();
                    jobTO.setType(JobType.CUSTOM);
                    jobTO.setRefKey(jobKey.getName());
                    jobTO.setRefDesc(jobDetail.getJobClass().getName());
                }
            }

            if (jobTO != null) {
                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                if (jobTriggers.isEmpty()) {
                    jobTO.setScheduled(false);
                } else {
                    jobTO.setScheduled(true);
                    jobTO.setStart(jobTriggers.get(0).getStartTime().toInstant().atOffset(FormatUtils.DEFAULT_OFFSET));
                }

                jobTO.setRunning(jobManager.isRunning(jobKey));

                jobTO.setStatus("UNKNOWN");
                if (jobTO.isRunning()) {
                    try {
                        Object job = ApplicationContextProvider.getBeanFactory().getBean(jobKey.getName());
                        if (job instanceof AbstractInterruptableJob
                                && ((AbstractInterruptableJob) job).getDelegate() != null) {

                            jobTO.setStatus(((AbstractInterruptableJob) job).getDelegate().currentStatus());
                        }
                    } catch (NoSuchBeanDefinitionException e) {
                        LOG.warn("Could not find job {} implementation", jobKey, e);
                    }
                }
            }
        }

        return jobTO;
    }

    protected List<JobTO> doListJobs(final boolean includeCustom) {
        List<JobTO> jobTOs = new ArrayList<>();
        try {
            for (JobKey jobKey : scheduler.getScheduler().
                    getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP))) {

                JobTO jobTO = getJobTO(jobKey, includeCustom);
                if (jobTO != null) {
                    jobTOs.add(jobTO);
                }
            }
        } catch (SchedulerException e) {
            LOG.debug("Problems while retrieving scheduled jobs", e);
        }

        return jobTOs;
    }

    protected void doActionJob(final JobKey jobKey, final JobAction action) {
        try {
            if (scheduler.getScheduler().checkExists(jobKey)) {
                switch (action) {
                    case START:
                        scheduler.getScheduler().triggerJob(jobKey);
                        break;

                    case STOP:
                        scheduler.getScheduler().interrupt(jobKey);
                        break;

                    case DELETE:
                        scheduler.getScheduler().deleteJob(jobKey);
                        break;

                    default:
                }
            } else {
                LOG.warn("Could not find job {}", jobKey);
            }
        } catch (SchedulerException e) {
            LOG.debug("Problems during {} operation on job {}", action.toString(), jobKey, e);
        }
    }
}
