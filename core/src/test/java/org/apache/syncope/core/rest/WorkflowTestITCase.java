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

import org.apache.syncope.common.types.WorkflowTasks;
import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.junit.Assume;
import org.junit.Test;

public class WorkflowTestITCase extends AbstractTest {

    @Test
    public void getUserDefinition() {
        WorkflowDefinitionTO definition = workflowService.getDefinition(AttributableType.USER);
        assertNotNull(definition);
    }

    @Test
    public void getRoleDefinition() {
        WorkflowDefinitionTO definition = workflowService.getDefinition(AttributableType.ROLE);
        assertNotNull(definition);
    }

    @Test
    public void updateUserDefinition() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        WorkflowDefinitionTO definition = workflowService.getDefinition(AttributableType.USER);
        assertNotNull(definition);

        workflowService.updateDefinition(AttributableType.USER, definition);
        WorkflowDefinitionTO newDefinition = workflowService.getDefinition(AttributableType.USER);
        assertNotNull(newDefinition);
    }

    @Test
    public void updateRoleDefinition() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForRoles());

        WorkflowDefinitionTO definition = workflowService.getDefinition(AttributableType.ROLE);
        assertNotNull(definition);

        workflowService.updateDefinition(AttributableType.ROLE, definition);
        WorkflowDefinitionTO newDefinition = workflowService.getDefinition(AttributableType.ROLE);
        assertNotNull(newDefinition);
    }

    @Test
    public void getUserTasks() {
        WorkflowTasks tasks = workflowService.getDefinedTasks(AttributableType.USER);
        assertNotNull(tasks.getTasks());
        assertFalse(tasks.getTasks().isEmpty());
    }

    @Test
    public void getRoleTasks() {
        WorkflowTasks tasks = workflowService.getDefinedTasks(AttributableType.ROLE);
        assertNotNull(tasks.getTasks());
        assertFalse(tasks.getTasks().isEmpty());
    }
}
