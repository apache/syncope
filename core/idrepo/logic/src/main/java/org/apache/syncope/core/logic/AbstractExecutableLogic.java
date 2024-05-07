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
import java.util.List;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public abstract class AbstractExecutableLogic<T extends EntityTO> extends AbstractJobLogic<T> {

    public AbstractExecutableLogic(
            final JobManager jobManager,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO) {

        super(jobManager, scheduler, jobStatusDAO);
    }

    public abstract ExecTO execute(ExecSpecs specs);

    public abstract Page<ExecTO> listExecutions(
            String key,
            OffsetDateTime before,
            OffsetDateTime after,
            Pageable pageable);

    public abstract List<ExecTO> listRecentExecutions(int max);

    public abstract ExecTO deleteExecution(String executionKey);

    public abstract List<BatchResponseItem> deleteExecutions(
            String key,
            OffsetDateTime before,
            OffsetDateTime after);

    public abstract JobTO getJob(String key);

    public abstract List<JobTO> listJobs();

    public abstract void actionJob(String key, JobAction action);
}
