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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.junit.Assume;
import org.junit.Test;

public class WorkflowTestITCase extends AbstractTest {

    public static final String ROLE_TYPE = "role";

    public static final String USER_TYPE = "user";

    @Test //TODO TestCase needs to be extended
    public void testGetUserDefinition() {
        WorkflowDefinitionTO definition = workflowService.getDefinition(USER_TYPE);
        assertNotNull(definition);
    }

    @Test //TODO TestCase needs to be extended
    public void testGetRoleDefinition() {
        WorkflowDefinitionTO definition = workflowService.getDefinition(ROLE_TYPE);
        assertNotNull(definition);
    }

    @Test//TODO TestCase needs to be extended
    public void testUpdateUserDefinition() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        WorkflowDefinitionTO definition = workflowService.getDefinition(USER_TYPE);
        assertNotNull(definition);

        workflowService.updateDefinition(USER_TYPE, definition);
        WorkflowDefinitionTO newDefinition = workflowService.getDefinition(USER_TYPE);
        assertNotNull(newDefinition);
    }

    @Test//TODO TestCase needs to be extended
    public void testUpdateRoleDefinition() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForRoles());

        WorkflowDefinitionTO definition = workflowService.getDefinition(ROLE_TYPE);
        assertNotNull(definition);

        workflowService.updateDefinition(ROLE_TYPE, definition);
        WorkflowDefinitionTO newDefinition = workflowService.getDefinition(ROLE_TYPE);
        assertNotNull(newDefinition);
    }

    @Test
    public void testGetUserTasks() {
        List<String> tasks = workflowService.getDefinedTasks(USER_TYPE);
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
    }

    @Test
    public void testGetRoleTasks() {
        List<String> tasks = workflowService.getDefinedTasks(ROLE_TYPE);
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
    }
}