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
package org.apache.syncope.core.provisioning.camel.producer;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;

public class StatusProducer extends AbstractProducer {

    private final UserDAO userDAO;

    private final UserWorkflowAdapter uwfAdapter;

    public StatusProducer(
            final Endpoint endpoint,
            final AnyTypeKind anyTypeKind,
            final UserDAO userDAO,
            final UserWorkflowAdapter uwfAdapter) {

        super(endpoint, anyTypeKind);
        this.userDAO = userDAO;
        this.uwfAdapter = uwfAdapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        if (getAnyTypeKind() == AnyTypeKind.USER && isPull()) {
            WorkflowResult<Map.Entry<UserUR, Boolean>> updated =
                    (WorkflowResult<Entry<UserUR, Boolean>>) exchange.getIn().getBody();

            Boolean enabled = exchange.getProperty("enabled", Boolean.class);
            String key = exchange.getProperty("key", String.class);
            String updater = exchange.getProperty("updater", String.class);
            String context = exchange.getProperty("context", String.class);

            if (enabled != null) {
                User user = userDAO.find(key);

                WorkflowResult<String> enableUpdate = null;
                if (user.isSuspended() == null) {
                    enableUpdate = uwfAdapter.activate(key, null, updater, context);
                } else if (enabled && user.isSuspended()) {
                    enableUpdate = uwfAdapter.reactivate(key, updater, context);
                } else if (!enabled && !user.isSuspended()) {
                    enableUpdate = uwfAdapter.suspend(key, updater, context);
                }

                if (enableUpdate != null) {
                    if (enableUpdate.getPropByRes() != null) {
                        updated.getPropByRes().merge(enableUpdate.getPropByRes());
                        updated.getPropByRes().purge();
                    }
                    updated.getPerformedTasks().addAll(enableUpdate.getPerformedTasks());
                }
            }
        } else if (getAnyTypeKind() == AnyTypeKind.USER) {
            WorkflowResult<String> updated = (WorkflowResult<String>) exchange.getIn().getBody();
            StatusR statusR = exchange.getProperty("statusR", StatusR.class);
            Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

            PropagationByResource<String> propByRes = new PropagationByResource<>();
            propByRes.addAll(ResourceOperation.UPDATE, statusR.getResources());

            PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
            userDAO.findLinkedAccounts(statusR.getKey()).stream().
                    filter(account -> statusR.getResources().contains(account.getResource().getKey())).
                    forEach(account -> propByLinkedAccount.add(
                    ResourceOperation.UPDATE,
                    Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

            List<PropagationTaskInfo> taskInfos = getPropagationManager().getUpdateTasks(
                    AnyTypeKind.USER,
                    statusR.getKey(),
                    false,
                    statusR.getType() != StatusRType.SUSPEND,
                    propByRes,
                    propByLinkedAccount,
                    null,
                    null);
            PropagationReporter reporter =
                    getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

            exchange.getMessage().setBody(Pair.of(updated.getResult(), reporter.getStatuses()));
        }
    }
}
