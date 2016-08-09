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

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;

public class ProvisionProducer extends AbstractProducer {

    public ProvisionProducer(final Endpoint endpoint, final AnyTypeKind anyType) {
        super(endpoint, anyType);
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

            UserPatch userPatch = new UserPatch();
            userPatch.setKey(key);
            userPatch.getResources().addAll(CollectionUtils.collect(resources,
                    new Transformer<String, StringPatchItem>() {

                @Override
                public StringPatchItem transform(final String resource) {
                    return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
                }
            }));

            if (changePwd) {
                userPatch.setPassword(
                        new PasswordPatch.Builder().onSyncope(true).value(password).resources(resources).build());
            }

            PropagationByResource propByRes = new PropagationByResource();
            for (String resource : resources) {
                propByRes.add(ResourceOperation.UPDATE, resource);
            }

            WorkflowResult<Pair<UserPatch, Boolean>> wfResult = new WorkflowResult<Pair<UserPatch, Boolean>>(
                    ImmutablePair.of(userPatch, (Boolean) null), propByRes, "update");

            List<PropagationTask> tasks = getPropagationManager().getUserUpdateTasks(wfResult, changePwd, null);
            PropagationReporter propagationReporter = getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);

            exchange.getOut().setBody(propagationReporter.getStatuses());
        } else {
            PropagationByResource propByRes = new PropagationByResource();
            propByRes.addAll(ResourceOperation.UPDATE, resources);

            AnyTypeKind anyTypeKind = AnyTypeKind.GROUP;
            if (getAnyTypeKind() != null) {
                anyTypeKind = getAnyTypeKind();
            }

            List<PropagationTask> tasks = getPropagationManager().getUpdateTasks(
                    anyTypeKind,
                    key,
                    false,
                    null,
                    propByRes,
                    null,
                    null);
            PropagationReporter propagationReporter = getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);

            exchange.getOut().setBody(propagationReporter.getStatuses());
        }
    }

}
