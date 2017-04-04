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

import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.flowable.FlowableUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Create extends AbstractFlowableServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    protected void doExecute(final String executionId) {
        UserTO userTO = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.USER_TO, UserTO.class);
        Boolean storePassword = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.STORE_PASSWORD, Boolean.class);
        // create and set workflow id
        User user = entityFactory.newEntity(User.class);
        dataBinder.create(user, userTO, storePassword == null ? true : storePassword);
        user.setWorkflowId(executionId);

        // report user as result
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.USER, user);
    }
}
