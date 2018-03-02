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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class IdentityRecertification extends AbstractSchedTaskJobDelegate {

    private static final String RECERTIFICATION_TIME = "identity.recertification.day.interval";

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    private long recertificationTime = -1;

    protected void init() {
        synchronized (this) {
            if (recertificationTime == -1) {
                CPlainAttr recertificationTimeAttr = confDAO.find(RECERTIFICATION_TIME);
                if (recertificationTimeAttr == null
                        || recertificationTimeAttr.getValues().get(0).getLongValue() == null) {

                    recertificationTime = -1;
                    return;
                }

                recertificationTime = recertificationTimeAttr.getValues().get(0).getLongValue() * 1000 * 60 * 60 * 24;
            }
        }
    }

    protected boolean isToBeRecertified(final User user, final long now) {
        if (!user.isSuspended()
                && (user.getLastRecertification() == null
                || user.getLastRecertification().getTime() + recertificationTime < now)) {

            LOG.debug("{} is to be recertified", user);
            return true;
        }

        LOG.debug("{} does not need to be recertified", user);
        return false;
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        LOG.info("IdentityRecertification {} running [SchedTask {}]", dryRun ? "dry " : "", task.getKey());

        init();
        if (recertificationTime == -1) {
            LOG.debug("Identity Recertification disabled");
            return ("IDENTITY RECERTIFICATION DISABLED");
        }

        if (dryRun) {
            return "DRY RUN";
        }

        int total = userDAO.count();
        int pages = (total / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

        status.set("Processing " + total + " users in " + pages + " pages");

        long now = System.currentTimeMillis();
        for (int page = 1; page <= pages; page++) {
            status.set("Processing " + total + " users: page " + page + " of " + pages);

            for (User user : userDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                LOG.debug("Processing user: {}", user.getUsername());

                if (StringUtils.isNotBlank(user.getWorkflowId()) && isToBeRecertified(user, now)) {
                    uwfAdapter.requestCertify(user);
                } else {
                    LOG.warn("Workflow for {} is null or empty", user);
                }
            }
        }

        return "SUCCESS";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }

}
