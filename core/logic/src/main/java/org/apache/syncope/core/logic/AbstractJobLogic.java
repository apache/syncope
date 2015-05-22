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
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.to.AbstractExecTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

abstract class AbstractJobLogic<T extends AbstractBaseBean> extends AbstractTransactionalLogic<T> {

    @Autowired
    protected SchedulerFactoryBean scheduler;

    protected abstract Long getKeyFromJobName(final JobKey jobKey);

    public <E extends AbstractExecTO> List<E> list(final JobStatusType type, final Class<E> reference) {
        List<E> jobExecTOs = new ArrayList<E>();

        switch (type) {
            case ALL:
                try {
                    for (String groupName : scheduler.getScheduler().getJobGroupNames()) {
                        for (JobKey jobKey : scheduler.getScheduler().getJobKeys(GroupMatcher.
                                jobGroupEquals(groupName))) {

                            Long jobId = getKeyFromJobName(jobKey);
                            if (jobId != null) {
                                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                                if (jobTriggers.size() > 0) {
                                    for (Trigger t : jobTriggers) {
                                        E jobExecTO = reference.newInstance();
                                        jobExecTO.setKey(jobId);
                                        jobExecTO.
                                                setStatus(scheduler.getScheduler().getTriggerState(t.getKey()).name());
                                        jobExecTO.setStartDate(t.getStartTime());
                                        jobExecTOs.add(jobExecTO);
                                    }
                                } else {
                                    E jobExecTO = reference.newInstance();
                                    jobExecTO.setKey(jobId);
                                    jobExecTO.setStatus("Not Scheduled");
                                    jobExecTOs.add(jobExecTO);
                                }
                            }
                        }
                    }
                } catch (SchedulerException ex) {
                    LOG.debug("Problems during retrieving all scheduled jobs {}", ex);
                } catch (InstantiationException ex) {
                    LOG.debug("Problems during instantiating {}  {}", reference, ex);
                } catch (IllegalAccessException ex) {
                    LOG.debug("Problems during accessing {}  {}", reference, ex);
                }
                break;
            case RUNNING:
                try {
                    for (JobExecutionContext jec : scheduler.getScheduler().getCurrentlyExecutingJobs()) {
                        Long jobId = getKeyFromJobName(jec.getJobDetail().getKey());
                        if (jobId != null) {
                            E jobExecTO = reference.newInstance();
                            jobExecTO.setKey(jobId);
                            jobExecTO.setStatus(scheduler.getScheduler().getTriggerState(jec.getTrigger().getKey()).
                                    name());
                            jobExecTO.setStartDate(jec.getFireTime());
                            jobExecTOs.add(jobExecTO);
                        }
                    }
                } catch (SchedulerException ex) {
                    LOG.debug("Problems during retrieving all currently executing jobs {}", ex);
                } catch (InstantiationException ex) {
                    LOG.debug("Problems during instantiating {}  {}", reference, ex);
                } catch (IllegalAccessException ex) {
                    LOG.debug("Problems during accessing {}  {}", reference, ex);
                }
                break;
            case SCHEDULED:
                try {
                    for (String groupName : scheduler.getScheduler().getJobGroupNames()) {
                        for (JobKey jobKey : scheduler.getScheduler().getJobKeys(GroupMatcher.
                                jobGroupEquals(groupName))) {
                            Long jobId = getKeyFromJobName(jobKey);
                            if (jobId != null) {
                                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                                for (Trigger t : jobTriggers) {
                                    E jobExecTO = reference.newInstance();
                                    jobExecTO.setKey(jobId);
                                    jobExecTO.setStatus(scheduler.getScheduler().getTriggerState(t.getKey()).name());
                                    jobExecTO.setStartDate(t.getStartTime());
                                    jobExecTOs.add(jobExecTO);
                                }
                            }
                        }
                    }
                } catch (SchedulerException ex) {
                    LOG.debug("Problems during retrieving all scheduled jobs {}", ex);
                } catch (InstantiationException ex) {
                    LOG.debug("Problems during instantiating {}  {}", reference, ex);
                } catch (IllegalAccessException ex) {
                    LOG.debug("Problems during accessing {}  {}", reference, ex);
                }
                break;
            default:
        }
        return jobExecTOs;
    }

    protected void process(final JobAction action, final String jobName) {

        if (jobName != null) {
            JobKey jobKey = new JobKey(jobName, Scheduler.DEFAULT_GROUP);
            try {
                if (scheduler.getScheduler().checkExists(jobKey)) {
                    switch (action) {
                        case START:
                            scheduler.getScheduler().triggerJob(jobKey);
                            break;
                        case STOP:
                            scheduler.getScheduler().interrupt(jobKey);
                            break;
                        default:
                    }
                }
            } catch (SchedulerException ex) {
                LOG.debug("Problems during {} operation on job with id {}", action.toString(), ex);
            }
        }
    }
}
