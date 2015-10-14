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
package org.apache.syncope.core.workflow.user.activiti.task;

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.user.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Update extends AbstractActivitiServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Override
    protected void doExecute(final String executionId) {
        SyncopeUser user =
                runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.SYNCOPE_USER, SyncopeUser.class);
        UserMod userMod =
                runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.USER_MOD, UserMod.class);

        PropagationByResource propByRes = dataBinder.update(user, userMod);

        // report updated user and propagation by resource as result
        runtimeService.setVariable(executionId, ActivitiUserWorkflowAdapter.SYNCOPE_USER, user);
        runtimeService.setVariable(executionId, ActivitiUserWorkflowAdapter.USER_MOD, userMod);
        runtimeService.setVariable(executionId, ActivitiUserWorkflowAdapter.PROP_BY_RESOURCE, propByRes);
    }
}
