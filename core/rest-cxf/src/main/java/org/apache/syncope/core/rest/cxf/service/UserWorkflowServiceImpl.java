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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.rest.api.service.UserWorkflowService;
import org.apache.syncope.core.logic.UserWorkflowLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserWorkflowServiceImpl implements UserWorkflowService {

    @Autowired
    private UserWorkflowLogic lofic;

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        return lofic.claimForm(taskId);
    }

    @Override
    public UserTO executeTask(final String taskId, final UserTO userTO) {
        return lofic.executeWorkflowTask(userTO, taskId);
    }

    @Override
    public WorkflowFormTO getFormForUser(final String userKey) {
        return lofic.getFormForUser(userKey);
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return lofic.getForms();
    }

    @Override
    public UserTO submitForm(final WorkflowFormTO form) {
        return lofic.submitForm(form);
    }
}
