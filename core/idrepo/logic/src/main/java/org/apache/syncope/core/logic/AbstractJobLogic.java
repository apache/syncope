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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.job.SystemLoadReporterJob;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.spring.security.AuthContextUtils;

abstract class AbstractJobLogic<T extends EntityTO> extends AbstractTransactionalLogic<T> {

    protected final JobManager jobManager;

    protected final SyncopeTaskScheduler scheduler;

    protected final JobStatusDAO jobStatusDAO;

    protected AbstractJobLogic(
            final JobManager jobManager,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO) {

        this.jobManager = jobManager;
        this.scheduler = scheduler;
        this.jobStatusDAO = jobStatusDAO;
    }

    protected abstract Triple<JobType, String, String> getReference(String jobName);

    protected Optional<JobTO> getJobTO(final String jobName, final boolean includeCustom) {
        JobTO jobTO = null;

        if (scheduler.contains(AuthContextUtils.getDomain(), jobName)) {
            Triple<JobType, String, String> reference = getReference(jobName);
            if (reference != null) {
                jobTO = new JobTO();
                jobTO.setType(reference.getLeft());
                jobTO.setRefKey(reference.getMiddle());
                jobTO.setRefDesc(reference.getRight());
            } else if (includeCustom) {
                Optional<Class<?>> jobClass = scheduler.getJobClass(AuthContextUtils.getDomain(), jobName).
                        filter(jc -> !TaskJob.class.isAssignableFrom(jc)
                        && !ReportJob.class.isAssignableFrom(jc)
                        && !SystemLoadReporterJob.class.isAssignableFrom(jc)
                        && !NotificationJob.class.isAssignableFrom(jc));
                if (jobClass.isPresent()) {
                    jobTO = new JobTO();
                    jobTO.setType(JobType.CUSTOM);
                    jobTO.setRefKey(jobName);
                    jobTO.setRefDesc(jobClass.get().getName());
                }
            }

            if (jobTO != null) {
                Optional<OffsetDateTime> nextTrigger = scheduler.getNextTrigger(AuthContextUtils.getDomain(), jobName);
                if (nextTrigger.isPresent()) {
                    jobTO.setScheduled(true);
                    jobTO.setStart(nextTrigger.get());
                } else {
                    jobTO.setScheduled(false);
                }

                jobTO.setRunning(jobManager.isRunning(jobName));

                jobTO.setStatus(jobStatusDAO.findById(jobName).map(JobStatus::getStatus).orElse("UNKNOWN"));
            }
        }

        return Optional.ofNullable(jobTO);
    }

    protected List<JobTO> doListJobs(final boolean includeCustom) {
        List<JobTO> jobTOs = new ArrayList<>();
        for (String jobName : scheduler.getJobNames(AuthContextUtils.getDomain())) {
            getJobTO(jobName, includeCustom).ifPresent(jobTOs::add);
        }

        return jobTOs;
    }

    protected void doActionJob(final String jobName, final JobAction action) {
        if (scheduler.contains(AuthContextUtils.getDomain(), jobName)) {
            switch (action) {
                case START ->
                    scheduler.start(AuthContextUtils.getDomain(), jobName);

                case STOP ->
                    scheduler.cancel(AuthContextUtils.getDomain(), jobName);

                case DELETE ->
                    scheduler.delete(AuthContextUtils.getDomain(), jobName);

                default -> {
                }
            }
        } else {
            LOG.warn("Could not find job {}", jobName);
        }
    }
}
