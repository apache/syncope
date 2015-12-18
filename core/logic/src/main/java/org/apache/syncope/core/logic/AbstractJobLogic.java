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
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
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

    private <E extends AbstractExecTO> void setTaskOrReportKey(final E jobExecTO, final Long taskOrReportKey) {
        if (jobExecTO instanceof TaskExecTO) {
            ((TaskExecTO) jobExecTO).setTask(taskOrReportKey);
        } else if (jobExecTO instanceof ReportExecTO) {
            ((ReportExecTO) jobExecTO).setReport(taskOrReportKey);
        }
    }

    public <E extends AbstractExecTO> List<E> listJobs(final JobStatusType type, final Class<E> reference) {
        List<E> jobExecTOs = new ArrayList<>();

        switch (type) {
            case ALL:
                try {
                    for (String groupName : scheduler.getScheduler().getJobGroupNames()) {
                        for (JobKey jobKey
                                : scheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                            Long key = getKeyFromJobName(jobKey);
                            if (key != null) {
                                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                                if (jobTriggers.isEmpty()) {
                                    E jobExecTO = reference.newInstance();
                                    setTaskOrReportKey(jobExecTO, key);
                                    jobExecTO.setStatus("Not Scheduled");

                                    jobExecTOs.add(jobExecTO);
                                } else {
                                    for (Trigger t : jobTriggers) {
                                        E jobExecTO = reference.newInstance();
                                        jobExecTO.setKey(key);
                                        jobExecTO.
                                                setStatus(scheduler.getScheduler().getTriggerState(t.getKey()).name());
                                        jobExecTO.setStart(t.getStartTime());

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
                        Long key = getKeyFromJobName(jec.getJobDetail().getKey());
                        if (key != null) {
                            E jobExecTO = reference.newInstance();
                            setTaskOrReportKey(jobExecTO, key);
                            jobExecTO.setStatus(
                                    scheduler.getScheduler().getTriggerState(jec.getTrigger().getKey()).name());
                            jobExecTO.setStart(jec.getFireTime());

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

                            Long key = getKeyFromJobName(jobKey);
                            if (key != null) {
                                List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                                for (Trigger t : jobTriggers) {
                                    E jobExecTO = reference.newInstance();
                                    setTaskOrReportKey(jobExecTO, key);
                                    jobExecTO.setStatus(scheduler.getScheduler().getTriggerState(t.getKey()).name());
                                    jobExecTO.setStart(t.getStartTime());

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
                            Long currentKey = getKeyFromJobName(jobKey);
                            boolean found = false;
                            //Two or more equals jobs cannot be executed concurrently
                            for (int i = 0; i < scheduler.getScheduler().getCurrentlyExecutingJobs().size() && !found;
                                    i++) {
                                JobExecutionContext jec = scheduler.getScheduler().getCurrentlyExecutingJobs().get(i);
                                Long execJobKey = getKeyFromJobName(jec.getJobDetail().getKey());
                                if (execJobKey == currentKey) {
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
                LOG.debug("Problems during {} operation on job {}", action.toString(), jobName, e);
            }
        }
    }
}
