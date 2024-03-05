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

import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

public class JobStatusUpdater {

    protected static final Logger LOG = LoggerFactory.getLogger(JobStatusUpdater.class);

    protected final JobStatusDAO jobStatusDAO;

    protected final EntityFactory entityFactory;

    protected boolean initCompleted = false;

    public JobStatusUpdater(final JobStatusDAO jobStatusDAO, final EntityFactory entityFactory) {
        this.jobStatusDAO = jobStatusDAO;
        this.entityFactory = entityFactory;
    }

    public void initComplete() {
        initCompleted = true;
    }

    /**
     * It's important to note that responding to job status updates must be done in async mode, and via a separate
     * special thread executor that attempts to synchronize job execution serially by only making one thread active at a
     * given time. Not doing so will force the event executor to launch separate threads per each status update, which
     * would result in multiple concurrent INSERT operations on the database, and failing.
     *
     * @param event the event
     */
    @Async("jobStatusUpdaterThreadExecutor")
    @EventListener
    public void update(final JobStatusEvent event) {
        if (!initCompleted) {
            LOG.debug("Core initialization not yet completed, discarding {}", event);
            return;
        }

        if (event.getJobStatus() == null) {
            LOG.debug("Requesting to delete status for job '{}#{}', ignoring", event.getDomain(), event.getJobName());
        } else {
            LOG.debug("Updating job '{}#{}' with status '{}'",
                    event.getDomain(), event.getJobName(), event.getJobStatus());

            AuthContextUtils.runAsAdmin(event.getDomain(), () -> {
                JobStatus jobStatus = jobStatusDAO.findById(event.getJobName()).orElse(null);
                if (jobStatus == null) {
                    jobStatus = entityFactory.newEntity(JobStatus.class);
                    jobStatus.setKey(event.getJobName());
                }

                jobStatus.setStatus(event.getJobStatus());
                jobStatusDAO.save(jobStatus);
            });
        }
    }
}
