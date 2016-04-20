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
package org.apache.syncope.core.provisioning.camel.processor;

import java.util.Map;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserSetStatusInPullProcessor implements Processor {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) {
        WorkflowResult<Map.Entry<UserPatch, Boolean>> updated = (WorkflowResult) exchange.getIn().getBody();

        Boolean enabled = exchange.getProperty("enabled", Boolean.class);
        String key = exchange.getProperty("key", String.class);

        if (enabled != null) {
            User user = userDAO.find(key);

            WorkflowResult<String> enableUpdate = null;
            if (user.isSuspended() == null) {
                enableUpdate = uwfAdapter.activate(key, null);
            } else if (enabled && user.isSuspended()) {
                enableUpdate = uwfAdapter.reactivate(key);
            } else if (!enabled && !user.isSuspended()) {
                enableUpdate = uwfAdapter.suspend(key);
            }

            if (enableUpdate != null) {
                if (enableUpdate.getPropByRes() != null) {
                    updated.getPropByRes().merge(enableUpdate.getPropByRes());
                    updated.getPropByRes().purge();
                }
                updated.getPerformedTasks().addAll(enableUpdate.getPerformedTasks());
            }
        }
    }
}
