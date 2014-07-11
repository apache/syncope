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

import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.user.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Create extends AbstractActivitiServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Override
    protected void doExecute(final String executionId) {
        UserTO userTO = (UserTO) runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.USER_TO);
        Boolean storePassword = (Boolean) runtimeService.getVariable(executionId,
                ActivitiUserWorkflowAdapter.STORE_PASSWORD);
        // create and set workflow id
        SyncopeUser user = new SyncopeUser();
        dataBinder.create(user, userTO, storePassword == null ? true : storePassword);
        user.setWorkflowId(executionId);

        // report SyncopeUser as result
        runtimeService.setVariable(executionId, ActivitiUserWorkflowAdapter.SYNCOPE_USER, user);
    }
}
