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
import java.util.Set;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

public class CreateProducer extends AbstractProducer {

    public CreateProducer(final Endpoint endpoint, final AnyTypeKind anyTypeKind) {
        super(endpoint, anyTypeKind);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        if ((exchange.getIn().getBody() instanceof WorkflowResult)) {
            Object actual = exchange.getProperty("actual");
            Set<String> excludedResources = exchange.getProperty("excludedResources", Set.class);
            Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

            if (actual instanceof UserCR) {
                UserWorkflowResult<Pair<String, Boolean>> created =
                        (UserWorkflowResult<Pair<String, Boolean>>) exchange.getIn().getBody();

                List<PropagationTaskInfo> taskInfos = getPropagationManager().getUserCreateTasks(
                        created.getResult().getKey(),
                        ((UserCR) actual).getPassword(),
                        created.getResult().getValue(),
                        created.getPropByRes(),
                        created.getPropByLinkedAccount(),
                        ((UserCR) actual).getVirAttrs(),
                        excludedResources);
                PropagationReporter reporter = getPropagationTaskExecutor().
                        execute(taskInfos, nullPriorityAsync, getExecutor());

                exchange.getMessage().setBody(Pair.of(created.getResult().getKey(), reporter.getStatuses()));
            } else if (actual instanceof AnyCR) {
                WorkflowResult<String> created = (WorkflowResult<String>) exchange.getIn().getBody();

                if (actual instanceof GroupCR && isPull()) {
                    Map<String, String> groupOwnerMap = exchange.getProperty("groupOwnerMap", Map.class);
                    ((GroupCR) actual).getPlainAttr(StringUtils.EMPTY).
                            ifPresent(groupOwner -> groupOwnerMap.put(
                            created.getResult(), groupOwner.getValues().iterator().next()));

                    List<PropagationTaskInfo> taskInfos = getPropagationManager().getCreateTasks(
                            AnyTypeKind.GROUP,
                            created.getResult(),
                            null,
                            created.getPropByRes(),
                            ((AnyCR) actual).getVirAttrs(),
                            excludedResources);
                    getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

                    exchange.getMessage().setBody(Pair.of(created.getResult(), null));
                } else {
                    List<PropagationTaskInfo> taskInfos = getPropagationManager().getCreateTasks(
                            actual instanceof AnyObjectCR ? AnyTypeKind.ANY_OBJECT : AnyTypeKind.GROUP,
                            created.getResult(),
                            null,
                            created.getPropByRes(),
                            ((AnyCR) actual).getVirAttrs(),
                            excludedResources);
                    PropagationReporter reporter =
                            getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());

                    exchange.getMessage().setBody(Pair.of(created.getResult(), reporter.getStatuses()));
                }
            }
        }
    }
}
