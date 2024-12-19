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

import java.time.OffsetDateTime;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

public class ExpiredAccessTokenCleanup extends AbstractSchedTaskJobDelegate<SchedTask> {

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Override
    protected String doExecute(final JobExecutionContext context) {
        if (!context.isDryRun()) {
            int deleted = accessTokenDAO.deleteExpired(OffsetDateTime.now());
            LOG.debug("Successfully deleted {} expired access tokens", deleted);
        }

        return "SUCCESS";
    }
}
