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
import java.util.Optional;
import java.util.Set;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;

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

            if (actual instanceof UserTO) {
                WorkflowResult<Pair<String, Boolean>> created =
                        (WorkflowResult<Pair<String, Boolean>>) exchange.getIn().getBody();

                List<PropagationTask> tasks = getPropagationManager().getUserCreateTasks(
                        created.getResult().getKey(),
                        ((UserTO) actual).getPassword(),
                        created.getResult().getValue(),
                        created.getPropByRes(),
                        ((UserTO) actual).getVirAttrs(),
                        excludedResources);
                PropagationReporter propagationReporter =
                        getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);

                exchange.getOut().setBody(
                        Pair.of(created.getResult().getKey(), propagationReporter.getStatuses()));
            } else if (actual instanceof AnyTO) {
                WorkflowResult<String> created = (WorkflowResult<String>) exchange.getIn().getBody();

                if (actual instanceof GroupTO && isPull()) {
                    Map<String, String> groupOwnerMap = exchange.getProperty("groupOwnerMap", Map.class);
                    Optional<AttrTO> groupOwner = ((GroupTO) actual).getPlainAttr(StringUtils.EMPTY);
                    if (groupOwner.isPresent()) {
                        groupOwnerMap.put(created.getResult(), groupOwner.get().getValues().iterator().next());
                    }

                    List<PropagationTask> tasks = getPropagationManager().getCreateTasks(
                            AnyTypeKind.GROUP,
                            created.getResult(),
                            created.getPropByRes(),
                            ((AnyTO) actual).getVirAttrs(),
                            excludedResources);
                    getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);

                    exchange.getOut().setBody(Pair.of(created.getResult(), null));
                } else {
                    List<PropagationTask> tasks = getPropagationManager().getCreateTasks(
                            actual instanceof AnyObjectTO ? AnyTypeKind.ANY_OBJECT : AnyTypeKind.GROUP,
                            created.getResult(),
                            created.getPropByRes(),
                            ((AnyTO) actual).getVirAttrs(),
                            excludedResources);
                    PropagationReporter propagationReporter =
                            getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);

                    exchange.getOut().setBody(Pair.of(created.getResult(), propagationReporter.getStatuses()));
                }
            }
        }
    }

}
