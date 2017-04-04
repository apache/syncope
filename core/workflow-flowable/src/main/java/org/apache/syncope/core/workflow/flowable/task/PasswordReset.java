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
package org.apache.syncope.core.workflow.flowable.task;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.core.workflow.flowable.FlowableUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PasswordReset extends AbstractFlowableServiceTask {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserDataBinder dataBinder;

    @Override
    protected void doExecute(final String executionId) {
        User user = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.USER, User.class);
        String token = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.TOKEN, String.class);
        String password = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.PASSWORD, String.class);

        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().
                onSyncope(true).
                resources(CollectionUtils.collect(userDAO.findAllResources(user), EntityUtils.keyTransformer())).
                value(password).build());

        PropagationByResource propByRes = dataBinder.update(user, userPatch);

        // report updated user and propagation by resource as result
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.USER, user);
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.USER_PATCH, userPatch);
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.PROP_BY_RESOURCE, propByRes);
    }

}
