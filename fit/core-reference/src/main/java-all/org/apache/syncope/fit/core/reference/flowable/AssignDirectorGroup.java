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
package org.apache.syncope.fit.core.reference.flowable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.flowable.task.FlowableServiceTask;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.flowable.engine.delegate.DelegateExecution;

public class AssignDirectorGroup extends FlowableServiceTask {
    private final UserDataBinder dataBinder;
    private final UserDAO userDAO;

    public AssignDirectorGroup(final UserDataBinder dataBinder, final UserDAO userDAO) {
        this.dataBinder = dataBinder;
        this.userDAO = userDAO;
    }

    @Override
    protected void doExecute(final DelegateExecution execution) {
        User user = execution.getVariable(FlowableRuntimeUtils.USER, User.class);

        Boolean secondLevelApprove = execution.
                getVariable("secondLevelApprove", Boolean.class);
        if (Boolean.TRUE.equals(secondLevelApprove)) {
            user = userDAO.save(user);

            UserUR userUR = new UserUR();
            userUR.setKey(user.getKey());
            userUR.getMemberships().add(new MembershipUR.Builder("ebf97068-aa4b-4a85-9f01-680e8c4cf227").build());

            Pair<PropagationByResource<String>, PropagationByResource<Pair<String, String>>> propInfo =
                    dataBinder.update(user, userUR);

            // report updated user and propagation by resource as result
            execution.setVariable(FlowableRuntimeUtils.USER, user);
            execution.setVariable(FlowableRuntimeUtils.PROP_BY_RESOURCE, propInfo.getLeft());
            execution.setVariable(FlowableRuntimeUtils.PROP_BY_LINKEDACCOUNT, propInfo.getRight());
        } else {
            LOG.info("Second level was not approved, not assigning the director group to {}", user.getUsername());
        }
    }
}
