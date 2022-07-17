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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.engine.delegate.DelegateExecution;

public class PasswordReset extends FlowableServiceTask {

    protected final UserDataBinder dataBinder;

    protected final UserDAO userDAO;

    public PasswordReset(final UserDataBinder dataBinder, final UserDAO userDAO) {
        this.dataBinder = dataBinder;
        this.userDAO = userDAO;
    }

    @Override
    protected void doExecute(final DelegateExecution execution) {
        User user = execution.getVariable(FlowableRuntimeUtils.USER, User.class);
        String token = execution.getVariable(FlowableRuntimeUtils.TOKEN, String.class);
        String password = execution.getVariable(FlowableRuntimeUtils.PASSWORD, String.class);

        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();

        UserUR req = new UserUR.Builder(user.getKey()).
                password(new PasswordPatch.Builder().
                        onSyncope(true).
                        resources(userDAO.findAllResourceKeys(user.getKey())).
                        value(password).build()).
                build();

        Pair<PropagationByResource<String>, PropagationByResource<Pair<String, String>>> propInfo =
                dataBinder.update(user, req);

        // report updated user and propagation by resource as result
        execution.setVariable(FlowableRuntimeUtils.USER, user);
        execution.setVariable(FlowableRuntimeUtils.USER_UR, req);
        execution.setVariable(FlowableRuntimeUtils.PROP_BY_RESOURCE, propInfo.getLeft());
        execution.setVariable(FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, propInfo.getRight());
    }
}
