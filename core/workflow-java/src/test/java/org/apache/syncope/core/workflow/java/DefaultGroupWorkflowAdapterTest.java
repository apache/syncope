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
package org.apache.syncope.core.workflow.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DefaultGroupWorkflowAdapterTest extends AbstractTest {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private GroupWorkflowAdapter gwfAdapter;

    @Test
    void createGroup() {
        // Use the Builder pattern if available, or direct construction
        GroupCR groupCR = new GroupCR();
        groupCR.setName("testGroup");
        groupCR.setRealm(SyncopeConstants.ROOT_REALM);

        WorkflowResult<String> result = gwfAdapter.create(groupCR, "admin", "test");

        assertNotNull(result.getResult());
        Group group = groupDAO.findById(result.getResult()).orElseThrow();
        assertEquals("testGroup", group.getName());
    }

    @Test
    void createGroupWithInvalidRealm() {
        // Use direct construction with invalid realm
        GroupCR groupCR = new GroupCR();
        groupCR.setName("testGroup");
        groupCR.setRealm("/nonexistent");

        assertThrows(NoSuchElementException.class, () -> gwfAdapter.create(groupCR, "admin", "test"));
    }

    @Test
    void deleteGroup() {
        // First create a group
        GroupCR groupCR = new GroupCR();
        groupCR.setName("groupToDelete");
        groupCR.setRealm(SyncopeConstants.ROOT_REALM);

        WorkflowResult<String> createResult = gwfAdapter.create(groupCR, "admin", "test");
        assertNotNull(createResult.getResult());

        assertTrue(groupDAO.findById(createResult.getResult()).isPresent());

        // Now delete it
        gwfAdapter.delete(createResult.getResult(), "admin", "test");

        // Verify it's deleted
        assertTrue(groupDAO.findById(createResult.getResult()).isEmpty());
    }
}
