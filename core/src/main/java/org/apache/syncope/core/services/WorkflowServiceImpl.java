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
package org.apache.syncope.core.services;

import javax.ws.rs.BadRequestException;

import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.services.WorkflowTasks;
import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.rest.controller.WorkflowController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowServiceImpl extends AbstractServiceImpl implements WorkflowService, ContextAware {

    @Autowired
    private WorkflowController controller;

    @Override
    public WorkflowDefinitionTO getDefinition(final AttributableType kind) {
        switch (kind) {
            case USER:
                return controller.getUserDefinition();

            case ROLE:
                return controller.getRoleDefinition();

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public void updateDefinition(final AttributableType kind, final WorkflowDefinitionTO definition) {
        switch (kind) {
            case USER:
                controller.updateUserDefinition(definition);
                break;

            case ROLE:
                controller.updateRoleDefinition(definition);
                break;

            default:
                throw new BadRequestException();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowTasks getDefinedTasks(final AttributableType kind) {
        switch (kind) {
            case USER:
                return new WorkflowTasks(controller.getDefinedUserTasks());

            case ROLE:
                return new WorkflowTasks(controller.getDefinedRoleTasks());

            default:
                throw new BadRequestException();
        }
    }
}
