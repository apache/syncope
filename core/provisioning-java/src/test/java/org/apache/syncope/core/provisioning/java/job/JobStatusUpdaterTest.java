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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JobStatusUpdaterTest extends AbstractTest {

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private JobStatusDAO jobStatusDAO;

    @Test
    public void verifyUpdate() {
        String refDesc = "JobRefDesc-" + SecureRandomUtils.generateRandomNumber();

        JobStatusUpdater jobStatusUpdater = new JobStatusUpdater(jobStatusDAO, entityFactory);
        jobStatusUpdater.initComplete();

        jobStatusUpdater.update(new JobStatusEvent(this, SyncopeConstants.MASTER_DOMAIN, refDesc, "Started"));
        assertTrue(jobStatusDAO.findById(refDesc).isPresent());

        jobStatusUpdater.update(new JobStatusEvent(this, SyncopeConstants.MASTER_DOMAIN, refDesc, null));
        assertTrue(jobStatusDAO.findById(refDesc).isEmpty());
    }
}
