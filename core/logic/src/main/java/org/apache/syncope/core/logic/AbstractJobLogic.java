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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

abstract class AbstractJobLogic<T extends AbstractBaseBean> extends AbstractTransactionalLogic<T> {

    private static final Comparator<Boolean> BOOLEAN_COMPARATOR = ComparatorUtils.booleanComparator(true);

    @SuppressWarnings("unchecked")
    private static final Comparator<JobTO> CHAINED_COMPARATOR = ComparatorUtils.chainedComparator(
            new Comparator<JobTO>() {

        @Override
        public int compare(final JobTO job1, final JobTO job2) {
            return BOOLEAN_COMPARATOR.compare(job1.isRunning(), job2.isRunning());
        }
    },
            new Comparator<JobTO>() {

        @Override
        public int compare(final JobTO job1, final JobTO job2) {
            return BOOLEAN_COMPARATOR.compare(job1.isScheduled(), job2.isScheduled());
        }
    },
            new Comparator<JobTO>() {

        @Override
        public int compare(final JobTO job1, final JobTO job2) {
            int result;

            if (job1.getStart() == null && job2.getStart() == null) {
                result = 0;
            } else if (job1.getStart() == null) {
                result = -1;
            } else if (job2.getStart() == null) {
                result = 1;
            } else {
                result = job1.getStart().compareTo(job2.getStart());
            }

            return result;
        }
    });

    @Autowired
    protected JobManager jobManager;

    @Autowired
    protected SchedulerFactoryBean scheduler;

    protected abstract Pair<Long, String> getReference(final JobKey jobKey);

    protected List<JobTO> listJobs(final int max) {
        List<JobTO> jobTOs = new ArrayList<>();

        try {
            for (JobKey jobKey : scheduler.getScheduler().
                    getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP))) {

                JobTO jobTO = new JobTO();

                Pair<Long, String> reference = getReference(jobKey);
                if (reference != null) {
                    jobTOs.add(jobTO);

                    jobTO.setReferenceKey(reference.getLeft());
                    jobTO.setReferenceName(reference.getRight());

                    List<? extends Trigger> jobTriggers = scheduler.getScheduler().getTriggersOfJob(jobKey);
                    if (jobTriggers.isEmpty()) {
                        jobTO.setScheduled(false);
                    } else {
                        jobTO.setScheduled(true);
                        jobTO.setStart(jobTriggers.get(0).getStartTime());
                        jobTO.setStatus(scheduler.getScheduler().getTriggerState(jobTriggers.get(0).getKey()).name());
                    }

                    jobTO.setRunning(jobManager.isRunning(jobKey));
                }
            }
        } catch (SchedulerException e) {
            LOG.debug("Problems while retrieving scheduled jobs", e);
        }

        Collections.sort(jobTOs, CHAINED_COMPARATOR);
        return jobTOs.size() > max ? jobTOs.subList(0, max) : jobTOs;
    }

    protected void actionJob(final JobKey jobKey, final JobAction action) {
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
