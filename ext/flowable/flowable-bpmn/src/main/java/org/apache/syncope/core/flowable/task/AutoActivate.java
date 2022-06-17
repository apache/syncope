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

import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.flowable.engine.delegate.DelegateExecution;

public class AutoActivate extends FlowableServiceTask {

    protected final UserDataBinder dataBinder;

    protected final UserDAO userDAO;

    public AutoActivate(final UserDataBinder dataBinder, final UserDAO userDAO) {
        this.dataBinder = dataBinder;
        this.userDAO = userDAO;
    }

    @Override
    protected void doExecute(final DelegateExecution execution) {
        User user = execution.getVariable(FlowableRuntimeUtils.USER, User.class);
        UserTO userTO = execution.getVariable(FlowableRuntimeUtils.USER_TO, UserTO.class);
        if (userTO != null && userTO.getKey() != null && user.getKey() != null) {
            user = userDAO.save(user);

            UserUR req = AnyOperations.diff(userTO, dataBinder.getUserTO(user, true), false);
            // don't mess with password, as the cleartext values was already properly saved
            req.setPassword(null);

            dataBinder.update(user, req);

            execution.setVariable(FlowableRuntimeUtils.USER, user);
        }

        execution.setVariable(FlowableRuntimeUtils.PROPAGATE_ENABLE, Boolean.TRUE);
    }
}
