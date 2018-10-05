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

import org.apache.syncope.common.lib.patch.RelationshipPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.flowable.task.FlowableServiceTask;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateARelationship extends FlowableServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Autowired
    private UserDAO userDAO;

    @Override
    protected void doExecute(final DelegateExecution execution) {
        User user = execution.
                getVariable(FlowableRuntimeUtils.USER, User.class);

        Boolean approve = execution.getVariable("approve", Boolean.class);
        if (Boolean.TRUE.equals(approve)) {
            user = userDAO.save(user);

            String printer = execution.getVariable("printer", String.class);

            UserPatch userPatch = new UserPatch();
            userPatch.setKey(user.getKey());
            userPatch.getRelationships().add(new RelationshipPatch.Builder().
                    relationshipTO(new RelationshipTO.Builder().
                            otherEnd("PRINTER", printer).type("neighborhood").build()).
                    build());

            PropagationByResource propByRes = dataBinder.update(user, userPatch);

            // report updated user and propagation by resource as result
            execution.setVariable(FlowableRuntimeUtils.USER, user);
            execution.setVariable(FlowableRuntimeUtils.PROP_BY_RESOURCE, propByRes);
        } else {
            LOG.info("Printer assignment to " + user.getUsername() + " was not approved");
        }
    }
}
