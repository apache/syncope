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
package org.apache.syncope.core.flowable.task;

import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.flowable.engine.delegate.DelegateExecution;

public class Create extends FlowableServiceTask {

    protected final UserDataBinder dataBinder;

    protected final EntityFactory entityFactory;

    public Create(final UserDataBinder dataBinder, final EntityFactory entityFactory) {
        this.dataBinder = dataBinder;
        this.entityFactory = entityFactory;
    }

    @Override
    protected void doExecute(final DelegateExecution execution) {
        UserCR userCR = execution.getVariable(FlowableRuntimeUtils.USER_CR, UserCR.class);

        // create user
        User user = entityFactory.newEntity(User.class);
        dataBinder.create(user, userCR);

        // report user as result
        execution.setVariable(FlowableRuntimeUtils.USER, user);
        execution.setVariable(FlowableRuntimeUtils.USER_TO, dataBinder.getUserTO(user, true));
    }
}
