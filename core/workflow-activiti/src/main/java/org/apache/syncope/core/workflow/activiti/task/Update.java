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
package org.apache.syncope.core.workflow.activiti.task;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Update extends AbstractActivitiServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Override
    protected void doExecute(final String executionId) {
        User user = engine.getRuntimeService().
                getVariable(executionId, ActivitiUserWorkflowAdapter.USER, User.class);
        UserPatch userPatch = engine.getRuntimeService().
                getVariable(executionId, ActivitiUserWorkflowAdapter.USER_PATCH, UserPatch.class);

        // update password internally only if required
        UserPatch updatedPatch = SerializationUtils.clone(userPatch);
        PasswordPatch updatedPwd = updatedPatch.getPassword();
        if (updatedPatch.getPassword() != null && !updatedPatch.getPassword().isOnSyncope()) {
            updatedPatch.setPassword(null);
        }
        // update user
        PropagationByResource propByRes = dataBinder.update(user, updatedPatch);
        if (updatedPatch.getPassword() != null && !updatedPatch.getPassword().getResources().isEmpty()) {
            if (updatedPwd == null) {
                updatedPwd = updatedPatch.getPassword();
            } else {
                updatedPwd.getResources().addAll(updatedPatch.getPassword().getResources());
            }
        }
        updatedPatch.setPassword(updatedPwd);

        // report updated user and propagation by resource as result
        engine.getRuntimeService().setVariable(executionId, ActivitiUserWorkflowAdapter.USER, user);
        engine.getRuntimeService().setVariable(executionId, ActivitiUserWorkflowAdapter.USER_PATCH, updatedPatch);
        engine.getRuntimeService().setVariable(executionId, ActivitiUserWorkflowAdapter.PROP_BY_RESOURCE, propByRes);
    }
}
