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

import java.util.Date;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class IdentityRecertification extends AbstractSchedTaskJobDelegate {

    @Autowired
    private UserDAO userDao;

    @Autowired 
    private UserWorkflowAdapter engine;
    
    @Autowired
    private ConfDAO confDAO;

    private long recertificationTimeLong = -1;

    public static final String RECERTIFICATION_TIME = "identity.recertification.day.interval";

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {

        LOG.info("TestIdentityRecertification {} running [SchedTask {}]", (dryRun
              ? "dry "
              : ""), task.getKey());
        init();

        if (recertificationTimeLong == -1) {
            LOG.debug("Identity Recertification disabled");
            return ("IDENTITY RECERT DISABLED");
        }

        for (User u :userDao.findAll()) {
            LOG.debug("Processing user: {}", u.getUsername());

            if (u.getWorkflowId() != null && !u.getWorkflowId().equals("")
                    && toBeRecertified(u) && !dryRun) {
                engine.requestCertify(u);
            } else {
                LOG.warn("Workflow for user: {} is null or empty", u.getUsername());
            }
        }

        return (dryRun
                ? "DRY "
                : "") + "RUNNING";
    }

    public void init() {
        CPlainAttr recertificationTime = confDAO.find(RECERTIFICATION_TIME);
        if (recertificationTime == null || recertificationTime.getValues().get(0).getLongValue() == null) {
            recertificationTimeLong = -1;
            return;
        }
        recertificationTimeLong = recertificationTime.getValues().get(0).getLongValue() * 1000 * 60 * 60 * 24;
    }

    public boolean toBeRecertified(final User user) {

        Date lastCertificationDate = user.getLastRecertification();

        if (lastCertificationDate != null) {
            if (lastCertificationDate.getTime() + recertificationTimeLong < System.currentTimeMillis()) {
                LOG.debug("User:  {}  to be recertified", user.getUsername());
                return true;
            } else {
                LOG.debug("User: {} do not needs to be recertified", user.getUsername());
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }

}
