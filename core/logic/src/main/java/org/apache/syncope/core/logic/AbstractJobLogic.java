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
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

abstract class AbstractJobLogic<T extends AbstractBaseBean> extends AbstractTransactionalLogic<T> {

    @Autowired
    protected JobManager jobManager;

    @Autowired
    protected SchedulerFactoryBean scheduler;

    protected abstract Triple<JobType, String, String> getReference(final JobKey jobKey);

    protected List<JobTO> doListJobs() {
        List<JobTO> jobTOs = new ArrayList<>();

        try {
            for (JobKey jobKey : scheduler.getScheduler().
                    getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP))) {

                JobTO jobTO = new JobTO();

                Triple<JobType, String, String> reference = getReference(jobKey);
                if (reference != null) {
                    jobTOs.add(jobTO);

                    jobTO.setType(reference.getLeft());
                    jobTO.setRefKey(reference.getMiddle());
                    jobTO.setRefDesc(reference.getRight());

                    List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                    if (jobTriggers.isEmpty()) {
                        jobTO.setScheduled(false);
                    } else {
                        jobTO.setScheduled(true);
                        jobTO.setStart(jobTriggers.get(0).getStartTime());
                    }

                    jobTO.setRunning(jobManager.isRunning(jobKey));
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
