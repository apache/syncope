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

import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AutoActivate extends AbstractActivitiServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Autowired
    private UserDAO userDAO;

    @Override
    protected void doExecute(final String executionId) {
        User user = engine.getRuntimeService().
                getVariable(executionId, ActivitiUserWorkflowAdapter.USER, User.class);
        UserTO userTO = engine.getRuntimeService().
                getVariable(executionId, ActivitiUserWorkflowAdapter.USER_TO, UserTO.class);
        if (userTO != null && userTO.getKey() != null && user.getKey() != null) {
            user = userDAO.save(user);

            UserPatch userPatch = AnyOperations.diff(userTO, dataBinder.getUserTO(user, true), false);
            // don't mess with password, as the cleartext values was already properly saved
            userPatch.setPassword(null);

            dataBinder.update(user, userPatch);

            engine.getRuntimeService().setVariable(executionId, ActivitiUserWorkflowAdapter.USER, user);
        }

        engine.getRuntimeService().setVariable(executionId, ActivitiUserWorkflowAdapter.PROPAGATE_ENABLE, Boolean.TRUE);
    }
}
