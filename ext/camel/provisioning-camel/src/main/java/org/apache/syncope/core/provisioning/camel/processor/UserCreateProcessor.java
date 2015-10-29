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

import java.util.List;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserCreateProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(UserCreateProcessor.class);

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) {
        if ((exchange.getIn().getBody() instanceof WorkflowResult)) {
            WorkflowResult<Pair<Long, Boolean>> created = (WorkflowResult) exchange.getIn().getBody();
            UserTO actual = exchange.getProperty("actual", UserTO.class);
            Set<String> excludedResources = exchange.getProperty("excludedResources", Set.class);
            Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

            List<PropagationTask> tasks = propagationManager.getUserCreateTasks(
                    created.getResult().getKey(),
                    actual.getPassword(),
                    created.getResult().getValue(),
                    created.getPropByRes(),
                    actual.getVirAttrs(),
                    excludedResources);
            PropagationReporter propagationReporter =
                    ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
            taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

            exchange.getOut().setBody(
                    new ImmutablePair<>(created.getResult().getKey(), propagationReporter.getStatuses()));
        }
    }

}
