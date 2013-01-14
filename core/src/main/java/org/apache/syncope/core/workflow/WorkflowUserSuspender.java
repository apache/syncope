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
package org.apache.syncope.core.workflow;

import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.policy.UserSuspender;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.apache.syncope.types.AccountPolicySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkflowUserSuspender implements UserSuspender {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowUserSuspender.class);

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private UserDataBinder userDataBinder;

    @Override
    public void suspend(AccountPolicySpec policy, SyncopeUser user) {
        try {
            LOG.debug("User {}:{} is over to max failed logins", user.getId(), user.getUsername());

            // reduce failed logins number to avoid multiple request
            user.setFailedLogins(user.getFailedLogins() - 1);

            // disable user
            final WorkflowResult<Long> updated = uwfAdapter.suspend(user);

            // propagate suspension if and only if it is required by policy
            if (policy.isPropagateSuspension()) {
                final List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                        new WorkflowResult<Map.Entry<Long, Boolean>>(
                        new SimpleEntry<Long, Boolean>(updated.getResult(), Boolean.FALSE),
                        updated.getPropByRes(), updated.getPerformedTasks()));

                taskExecutor.execute(tasks);
            }

            if (LOG.isDebugEnabled()) {
                final UserTO savedTO = userDataBinder.getUserTO(updated.getResult());
                LOG.debug("About to return suspended user\n{}", savedTO);
            }
        } catch (Exception e) {
            LOG.error("Error during user suspension", e);
        }    }

}
