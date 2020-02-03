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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

public class SuspendProducer extends AbstractProducer {

    public SuspendProducer(final Endpoint endpoint, final AnyTypeKind anyTypeKind) {
        super(endpoint, anyTypeKind);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        if (getAnyTypeKind() == AnyTypeKind.USER) {
            Pair<UserWorkflowResult<String>, Boolean> updated =
                    (Pair<UserWorkflowResult<String>, Boolean>) exchange.getIn().getBody();

            // propagate suspension if and only if it is required by policy
            if (updated != null && updated.getRight()) {
                UserUR userUR = new UserUR.Builder(updated.getLeft().getResult()).build();

                List<PropagationTaskInfo> taskInfos = getPropagationManager().getUserUpdateTasks(
                        new UserWorkflowResult<>(
                                Pair.of(userUR, Boolean.FALSE),
                                updated.getLeft().getPropByRes(),
                                updated.getKey().getPropByLinkedAccount(),
                                updated.getLeft().getPerformedTasks()));
                getPropagationTaskExecutor().execute(taskInfos, false, getExecutor());
            }
        }
    }
}
