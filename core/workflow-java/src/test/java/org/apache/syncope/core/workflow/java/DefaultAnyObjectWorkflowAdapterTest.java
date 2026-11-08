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
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DefaultAnyObjectWorkflowAdapterTest extends AbstractTest {

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnyObjectWorkflowAdapter awfAdapter;

    @Test
    void createAnyObject() {
        // Use the Builder pattern to set type, as required by AnyObjectCR
        AnyObjectCR anyObjectCR = new AnyObjectCR.Builder(
                SyncopeConstants.ROOT_REALM, "PRINTER", "testAnyObject").build();

        WorkflowResult<String> result = awfAdapter.create(anyObjectCR, "admin", "test");
        assertNotNull(result.getResult());

        AnyObject anyObject = anyObjectDAO.findById(result.getResult()).orElseThrow();
        assertEquals("testAnyObject", anyObject.getName());
    }

    @Test
    void createAnyObjectWithInvalidRealm() {
        // Use the Builder pattern with invalid realm - this tests the validation path
        AnyObjectCR anyObjectCR = new AnyObjectCR.Builder(
                "/nonexistent", "PRINTER", "testAnyObject").build();

        assertThrows(NoSuchElementException.class, () -> awfAdapter.create(anyObjectCR, "admin", "test"));
    }

    @Test
    void createAnyObjectWithInvalidAnyType() {
        // Use the Builder pattern with invalid any type - this tests the validation path
        AnyObjectCR anyObjectCR = new AnyObjectCR.Builder(
                SyncopeConstants.ROOT_REALM, "nonexistent", "testAnyObject").build();

        assertThrows(NoSuchElementException.class, () -> awfAdapter.create(anyObjectCR, "admin", "test"));
    }

    @Test
    void deleteAnyObject() {
        // First create an anyobject using Builder pattern
        AnyObjectCR anyObjectCR = new AnyObjectCR.Builder(
                SyncopeConstants.ROOT_REALM, "PRINTER", "anyObjectToDelete").build();

        WorkflowResult<String> createResult = awfAdapter.create(anyObjectCR, "admin", "test");

        assertTrue(anyObjectDAO.findById(createResult.getResult()).isPresent());

        // Now delete it using the key from the result
        awfAdapter.delete(createResult.getResult(), "admin", "test");

        // Verify it's deleted
        assertTrue(anyObjectDAO.findById(createResult.getResult()).isEmpty());
    }
}
