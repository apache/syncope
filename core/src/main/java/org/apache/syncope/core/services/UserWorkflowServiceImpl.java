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
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserWorkflowServiceImpl implements UserWorkflowService {

    @Autowired
    private UserController userController;

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        return userController.claimForm(taskId);
    }
    

    @Override
    public UserTO executeWorkflow(final String taskId, final UserTO userTO) {
        return userController.executeWorkflow(userTO, taskId);
    }

    @Override
    public WorkflowFormTO getFormForUser(final Long userId) {
        return userController.getFormForUser(userId);
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return userController.getForms();
    }
    

    @Override
    public UserTO submitForm(final WorkflowFormTO form) {
        return userController.submitForm(form);
    }
    
}
