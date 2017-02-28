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

import java.util.Collections;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class IdentityRecertification extends AbstractSchedTaskJobDelegate {

    private static final String RECERTIFICATION_TIME = "identity.recertification.day.interval";

    private static final int PAGE_SIZE = 10;

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

    protected boolean isToBeRecertified(final User user) {
        Date lastCertificationDate = user.getLastRecertification();

        if (lastCertificationDate != null) {
            if (lastCertificationDate.getTime() + recertificationTime < System.currentTimeMillis()) {
                LOG.debug("{} is to be recertified", user);
                return true;
            } else {
                LOG.debug("{} do not need to be recertified", user);
                return false;
            }
        }

        return true;
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        LOG.info("IdentityRecertification {} running [SchedTask {}]", (dryRun
                ? "dry "
                : ""), task.getKey());

        init();
        if (recertificationTime == -1) {
            LOG.debug("Identity Recertification disabled");
            return ("IDENTITY RECERTIFICATION DISABLED");
        }

        for (int page = 1; page <= (userDAO.count() / PAGE_SIZE) + 1; page++) {
            for (User user : userDAO.findAll(
                    SyncopeConstants.FULL_ADMIN_REALMS, page, PAGE_SIZE, Collections.<OrderByClause>emptyList())) {

                LOG.debug("Processing user: {}", user.getUsername());

                if (StringUtils.isNotBlank(user.getWorkflowId()) && isToBeRecertified(user) && !dryRun) {
                    uwfAdapter.requestCertify(user);
                } else {
                    LOG.warn("Workflow for {} is null or empty", user);
                }
            }
        }

        return (dryRun
                ? "DRY "
                : "") + "RUNNING";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }

}
