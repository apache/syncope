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
package org.apache.syncope.core.rest.controller;

import static org.apache.syncope.core.rest.controller.AbstractController.LOG;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.to.AbstractExecTO;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.types.JobAction;
import org.apache.syncope.common.types.JobStatusType;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

abstract class AbstractJobController<T extends AbstractBaseBean> extends AbstractTransactionalController<T> {

    @Autowired
    protected SchedulerFactoryBean scheduler;

    protected abstract Long getIdFromJobName(JobKey jobKey);

    private <E extends AbstractExecTO> void setTaskOrReportId(final E jobExecTO, final Long taskOrReportId) {
        if (jobExecTO instanceof TaskExecTO) {
            ((TaskExecTO) jobExecTO).setTask(taskOrReportId);
        } else if (jobExecTO instanceof ReportExecTO) {
            ((ReportExecTO) jobExecTO).setReport(taskOrReportId);
        }
    }

    public <E extends AbstractExecTO> List<E> listJobs(final JobStatusType type, final Class<E> reference) {
        List<E> jobExecTOs = new ArrayList<E>();

        switch (type) {
            case ALL:
                try {
                    for (String groupName : scheduler.getScheduler().getJobGroupNames()) {
                        for (JobKey jobKey
                                : scheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                            Long jobId = getIdFromJobName(jobKey);
                            if (jobId != null) {
                                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                                if (jobTriggers.isEmpty()) {
                                    E jobExecTO = reference.newInstance();
                                    setTaskOrReportId(jobExecTO, jobId);
                                    jobExecTO.setStatus("Not Scheduled");

                                    jobExecTOs.add(jobExecTO);
                                } else {
                                    for (Trigger t : jobTriggers) {
                                        E jobExecTO = reference.newInstance();
                                        setTaskOrReportId(jobExecTO, jobId);
                                        jobExecTO.setStatus(
                                                scheduler.getScheduler().getTriggerState(t.getKey()).name());
                                        jobExecTO.setStartDate(t.getStartTime());

                                        jobExecTOs.add(jobExecTO);
                                    }
                                }
                            }
                        }
                    }
                } catch (SchedulerException e) {
                    LOG.debug("Problems while retrieving all scheduled jobs", e);
                } catch (InstantiationException e) {
                    LOG.debug("Problems while instantiating {}", reference, e);
                } catch (IllegalAccessException e) {
                    LOG.debug("Problems while accessing {}", reference, e);
                }
                break;

            case RUNNING:
                try {
                    for (JobExecutionContext jec : scheduler.getScheduler().getCurrentlyExecutingJobs()) {
                        Long jobId = getIdFromJobName(jec.getJobDetail().getKey());
                        if (jobId != null) {
                            E jobExecTO = reference.newInstance();
                            setTaskOrReportId(jobExecTO, jobId);
                            jobExecTO.setStatus(
                                    scheduler.getScheduler().getTriggerState(jec.getTrigger().getKey()).name());
                            jobExecTO.setStartDate(jec.getFireTime());

                            jobExecTOs.add(jobExecTO);
                        }
                    }
                } catch (SchedulerException e) {
                    LOG.debug("Problems while retrieving all currently executing jobs", e);
                } catch (InstantiationException e) {
                    LOG.debug("Problems while instantiating {}", reference, e);
                } catch (IllegalAccessException e) {
                    LOG.debug("Problems while accessing {}", reference, e);
                }
                break;

            case SCHEDULED:
                try {
                    for (String groupName : scheduler.getScheduler().getJobGroupNames()) {
                        for (JobKey jobKey
                                : scheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                            Long jobId = getIdFromJobName(jobKey);
                            if (jobId != null) {
                                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                                for (Trigger t : jobTriggers) {
                                    E jobExecTO = reference.newInstance();
                                    setTaskOrReportId(jobExecTO, jobId);
                                    jobExecTO.setStatus(scheduler.getScheduler().getTriggerState(t.getKey()).name());
                                    jobExecTO.setStartDate(t.getStartTime());

                                    jobExecTOs.add(jobExecTO);
                                }
                            }
                        }
                    }
                } catch (SchedulerException e) {
                    LOG.debug("Problems while retrieving all scheduled jobs", e);
                } catch (InstantiationException e) {
                    LOG.debug("Problems while instantiating {}", reference, e);
                } catch (IllegalAccessException e) {
                    LOG.debug("Problems while accessing {}", reference, e);
                }
                break;

            default:
        }
        return jobExecTOs;
    }

    protected void actionJob(final String jobName, final JobAction action) {
        if (jobName != null) {
            JobKey jobKey = new JobKey(jobName, Scheduler.DEFAULT_GROUP);
            try {
                if (scheduler.getScheduler().checkExists(jobKey)) {
                    switch (action) {
                        case START:
                            Long currentId = getIdFromJobName(jobKey);
                            boolean found = false;
                            //Two or more equals jobs cannot be executed concurrently
                            for (int i = 0; i < scheduler.getScheduler().getCurrentlyExecutingJobs().size() && !found;
                                    i++) {
                                JobExecutionContext jec = scheduler.getScheduler().getCurrentlyExecutingJobs().get(i);
                                Long jobId = getIdFromJobName(jec.getJobDetail().getKey());
                                if (jobId == currentId) {
                                    found = true;
                                }
                            }
                            if (!found) {
                                scheduler.getScheduler().triggerJob(jobKey);
                            }
                            break;

                        case STOP:
                            scheduler.getScheduler().interrupt(jobKey);
                            break;

                        default:
                    }
                }
            } catch (SchedulerException e) {
                LOG.debug("Problems during {} operation on job with id {}", action.toString(), jobName, e);
            }
        }
    }
}
