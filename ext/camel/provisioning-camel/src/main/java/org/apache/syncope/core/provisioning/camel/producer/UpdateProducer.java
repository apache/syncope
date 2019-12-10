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
import java.util.Set;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

public class UpdateProducer extends AbstractProducer {

    public UpdateProducer(final Endpoint endpoint, final AnyTypeKind anyTypeKind) {
        super(endpoint, anyTypeKind);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        if ((exchange.getIn().getBody() instanceof WorkflowResult)) {
            Object actual = exchange.getProperty("actual");
            Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);
            Set<String> excludedResources = exchange.getProperty("excludedResources", Set.class);

            if (actual instanceof UserUR || isPull()) {
                UserWorkflowResult<Pair<UserUR, Boolean>> updated =
                        (UserWorkflowResult<Pair<UserUR, Boolean>>) exchange.getIn().getBody();

                List<PropagationTaskInfo> taskInfos;
                if (isPull()) {
                    boolean passwordNotNull = updated.getResult().getKey().getPassword() != null;
                    taskInfos = getPropagationManager().getUserUpdateTasks(updated, passwordNotNull, excludedResources);
                } else {
                    taskInfos = getPropagationManager().getUserUpdateTasks(updated);
                }
                PropagationReporter reporter =
                    getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

                exchange.getMessage().setBody(Pair.of(updated.getResult().getLeft(), reporter.getStatuses()));
            } else if (actual instanceof AnyUR) {
                WorkflowResult<? extends AnyUR> updated =
                        (WorkflowResult<? extends AnyUR>) exchange.getIn().getBody();

                List<PropagationTaskInfo> taskInfos = getPropagationManager().getUpdateTasks(
                        actual instanceof AnyObjectUR ? AnyTypeKind.ANY_OBJECT : AnyTypeKind.GROUP,
                        updated.getResult().getKey(),
                        false,
                        null,
                        updated.getPropByRes(),
                        null,
                        ((AnyUR) actual).getVirAttrs(),
                        excludedResources);
                PropagationReporter reporter =
                    getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

                exchange.getMessage().setBody(Pair.of(updated.getResult(), reporter.getStatuses()));
            }
        }
    }
}
