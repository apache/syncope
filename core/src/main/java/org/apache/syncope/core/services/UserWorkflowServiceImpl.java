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

import java.util.List;

import org.apache.syncope.common.services.UserWorkflowService;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.core.rest.controller.UserWorkflowController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserWorkflowServiceImpl implements UserWorkflowService {

    @Autowired
    private UserWorkflowController controller;

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        return controller.claimForm(taskId);
    }

    @Override
    public UserTO executeWorkflow(final String taskId, final UserTO userTO) {
        return controller.executeWorkflow(userTO, taskId);
    }

    @Override
    public WorkflowFormTO getFormForUser(final Long userId) {
        return controller.getFormForUser(userId);
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return controller.getForms();
    }

    @Override
    public UserTO submitForm(final WorkflowFormTO form) {
        return controller.submitForm(form);
    }

    @Override
    public List<WorkflowFormTO> getFormsByName(final Long userId, final String taskName) {
        return controller.getForms(userId, taskName);
    }
}
