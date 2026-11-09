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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RemediationDataBinderTest extends AbstractDataBinderTest {

    private static final String PULL_TASK_KEY = "c41b9b71-9bfa-4f90-89f2-84787def4c5c";

    @Autowired
    private RemediationDataBinder binder;

    @Autowired
    private RemediationDAO remediationDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private EntityFactory entityFactory;

    protected Remediation newRemediation(final ResourceOperation operation) {
        Remediation remediation = entityFactory.newEntity(Remediation.class);
        remediation.setAnyType(anyTypeDAO.findById(AnyTypeKind.USER.name()).orElseThrow());
        remediation.setOperation(operation);
        remediation.setError("error");
        remediation.setInstant(OffsetDateTime.now());
        remediation.setRemoteName("remoteName");

        PullTask pullTask = (PullTask) taskDAO.<SchedTask>findById(TaskType.PULL, PULL_TASK_KEY).orElseThrow();
        remediation.setPullTask(pullTask);

        return remediation;
    }

    @Test
    void createPayload() {
        Remediation remediation = newRemediation(ResourceOperation.CREATE);
        UserCR userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        userCR.setUsername("rossini");
        userCR.setPassword("password");
        remediation.setPayload(userCR);

        remediation = remediationDAO.save(remediation);
        entityManager.flush();

        RemediationTO actual = binder.getRemediationTO(
                remediationDAO.findById(remediation.getKey()).orElseThrow());
        assertEquals(remediation.getKey(), actual.getKey());
        assertEquals(AnyTypeKind.USER.name(), actual.getAnyType());
        assertEquals(ResourceOperation.CREATE, actual.getOperation());
        assertEquals("error", actual.getError());
        assertEquals("remoteName", actual.getRemoteName());
        assertEquals(PULL_TASK_KEY, actual.getPullTask());
        assertEquals("resource-csv", actual.getResource());
        assertTrue(actual.getAnyCRPayload() instanceof UserCR);
        assertEquals("rossini", ((UserCR) actual.getAnyCRPayload()).getUsername());
    }

    @Test
    void updatePayload() {
        Remediation remediation = newRemediation(ResourceOperation.UPDATE);
        UserUR userUR = new UserUR();
        userUR.setKey("1417acbe-cbf6-4277-9372-e75e04f97000");
        remediation.setPayload(userUR);

        remediation = remediationDAO.save(remediation);
        entityManager.flush();

        RemediationTO actual = binder.getRemediationTO(
                remediationDAO.findById(remediation.getKey()).orElseThrow());
        assertEquals(ResourceOperation.UPDATE, actual.getOperation());
        assertTrue(actual.getAnyURPayload() instanceof UserUR);
        assertEquals(userUR.getKey(), actual.getAnyURPayload().getKey());
    }

    @Test
    void deletePayload() {
        String key = "1417acbe-cbf6-4277-9372-e75e04f97000";

        Remediation remediation = newRemediation(ResourceOperation.DELETE);
        remediation.setPayload(key);

        remediation = remediationDAO.save(remediation);
        entityManager.flush();

        RemediationTO actual = binder.getRemediationTO(
                remediationDAO.findById(remediation.getKey()).orElseThrow());
        assertEquals(ResourceOperation.DELETE, actual.getOperation());
        assertEquals(key, actual.getKeyPayload());
    }

    @Test
    void noPayload() {
        Remediation remediation = newRemediation(ResourceOperation.NONE);
        remediation.setPayload("whatever");

        RemediationTO actual = binder.getRemediationTO(remediation);
        assertEquals(ResourceOperation.NONE, actual.getOperation());
        assertEquals(AnyTypeKind.USER.name(), actual.getAnyType());
        assertEquals(PULL_TASK_KEY, actual.getPullTask());
        assertEquals("resource-csv", actual.getResource());
    }
}
