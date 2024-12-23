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

import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.LiveSyncJobDelegate;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveSyncJob extends TaskJob {

    private static final Logger LOG = LoggerFactory.getLogger(LiveSyncJob.class);

    private LiveSyncJobDelegate delegate;

    @Override
    public SchedTaskJobDelegate getDelegate() {
        return delegate;
    }

    @Override
    protected void delegate(final JobExecutionContext context, final String taskKey)
            throws ClassNotFoundException, JobExecutionException {

        String implKey = (String) context.getData().get(JobManager.DELEGATE_IMPLEMENTATION);
        Implementation impl = implementationDAO.findById(implKey).orElse(null);
        if (impl == null) {
            LOG.error("Could not find Implementation '{}', aborting", implKey);
        } else {
            delegate = ImplementationManager.build(impl);
            delegate.execute(
                    TaskType.LIVE_SYNC,
                    taskKey,
                    context);
        }
    }
}
