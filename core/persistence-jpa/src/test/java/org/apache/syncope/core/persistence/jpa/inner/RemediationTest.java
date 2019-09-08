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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class RemediationTest extends AbstractTest {

    @Autowired
    private RemediationDAO remediationDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Test
    public void findAll() {
        List<Remediation> remediations = remediationDAO.findAll(1, 1, List.of());
        assertTrue(remediations.isEmpty());
    }

    @Test
    public void createMissingPayload() {
        Remediation remediation = entityFactory.newEntity(Remediation.class);
        remediation.setAnyType(anyTypeDAO.find("PRINTER"));
        remediation.setOperation(ResourceOperation.CREATE);
        remediation.setError("Error");
        remediation.setInstant(new Date());
        remediation.setRemoteName("remote");
        remediation.setPullTask(taskDAO.find("38abbf9e-a1a3-40a1-a15f-7d0ac02f47f1"));

        // missing payload
        try {
            remediationDAO.save(remediation);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            Set<EntityViolationType> violations = e.getViolations().values().iterator().next();
            assertEquals(2, violations.size());
            assertTrue(violations.stream().allMatch(violation -> violation.getPropertyPath().equals("payload")));
        }
    }

    @Test
    public void createWrongPayload() {
        Remediation remediation = entityFactory.newEntity(Remediation.class);
        remediation.setAnyType(anyTypeDAO.find("PRINTER"));
        remediation.setOperation(ResourceOperation.CREATE);
        remediation.setError("Error");
        remediation.setInstant(new Date());
        remediation.setRemoteName("remote");
        remediation.setPullTask(taskDAO.find("38abbf9e-a1a3-40a1-a15f-7d0ac02f47f1"));
        remediation.setPayload(UUID.randomUUID().toString());

        // wrong payload for operation
        try {
            remediationDAO.save(remediation);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            Set<EntityViolationType> violations = e.getViolations().values().iterator().next();
            assertEquals(1, violations.size());
            assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().equals("payload")));
        }
    }

    @Test
    public void create() {
        Remediation remediation = entityFactory.newEntity(Remediation.class);
        remediation.setAnyType(anyTypeDAO.find("PRINTER"));
        remediation.setOperation(ResourceOperation.CREATE);
        remediation.setError("Error");
        remediation.setInstant(new Date());
        remediation.setRemoteName("remote");
        remediation.setPullTask(taskDAO.find("38abbf9e-a1a3-40a1-a15f-7d0ac02f47f1"));
        remediation.setPayload(UUID.randomUUID().toString());
        remediation.setOperation(ResourceOperation.DELETE);

        remediation = remediationDAO.save(remediation);
        assertNotNull(remediation.getKey());
        assertNotNull(remediation.getPullTask());

        taskDAO.delete(remediation.getPullTask());

        entityManager().flush();

        remediation = remediationDAO.find(remediation.getKey());
        assertNull(remediation.getPullTask());
    }
}
