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
import java.util.stream.Collectors;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

public class ProvisionProducer extends AbstractProducer {

    private final UserDAO userDAO;

    public ProvisionProducer(final Endpoint endpoint, final AnyTypeKind anyType, final UserDAO userDAO) {
        super(endpoint, anyType);
        this.userDAO = userDAO;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        String key = exchange.getIn().getBody(String.class);
        List<String> resources = exchange.getProperty("resources", List.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        if (getAnyTypeKind() == AnyTypeKind.USER) {
            Boolean changePwd = exchange.getProperty("changePwd", Boolean.class);
            String password = exchange.getProperty("password", String.class);

            UserUR userUR = new UserUR();
            userUR.setKey(key);
            userUR.getResources().addAll(resources.stream().map(resource
                    -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
                    collect(Collectors.toList()));

            if (changePwd) {
                userUR.setPassword(
                        new PasswordPatch.Builder().onSyncope(true).value(password).resources(resources).build());
            }

            PropagationByResource<String> propByRes = new PropagationByResource<>();
            propByRes.addAll(ResourceOperation.UPDATE, resources);

            UserWorkflowResult<Pair<UserUR, Boolean>> wfResult = new UserWorkflowResult<>(
                    Pair.of(userUR, (Boolean) null), propByRes, null, "update");

            List<PropagationTaskInfo> taskInfos = getPropagationManager().getUserUpdateTasks(wfResult, changePwd, null);
            PropagationReporter reporter =
                getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

            exchange.getMessage().setBody(reporter.getStatuses());
        } else {
            PropagationByResource<String> propByRes = new PropagationByResource<>();
            propByRes.addAll(ResourceOperation.UPDATE, resources);

            PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
            userDAO.findLinkedAccounts(key).stream().
                    filter(account -> resources.contains(account.getResource().getKey())).
                    forEach(account -> propByLinkedAccount.add(
                    ResourceOperation.UPDATE,
                    Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

            AnyTypeKind anyTypeKind = AnyTypeKind.GROUP;
            if (getAnyTypeKind() != null) {
                anyTypeKind = getAnyTypeKind();
            }

            List<PropagationTaskInfo> taskInfos = getPropagationManager().getUpdateTasks(
                    anyTypeKind,
                    key,
                    false,
                    null,
                    propByRes,
                    propByLinkedAccount,
                    null,
                    null);
            PropagationReporter reporter =
                getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

            exchange.getMessage().setBody(reporter.getStatuses());
        }
    }
}
