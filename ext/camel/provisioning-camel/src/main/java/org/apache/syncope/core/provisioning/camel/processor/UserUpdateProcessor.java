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

import java.util.Collections;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserUpdateProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(UserUpdateProcessor.class);

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Override
    @SuppressWarnings("unchecked")
    public void process(final Exchange exchange) {
        WorkflowResult<Pair<UserMod, Boolean>> updated = (WorkflowResult) exchange.getIn().getBody();
        UserMod userMod = exchange.getProperty("actual", UserMod.class);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(updated);
        if (tasks.isEmpty()) {
            // SYNCOPE-459: take care of user virtual attributes ...
            PropagationByResource propByResVirAttr = virtAttrHandler.fillVirtual(
                    updated.getResult().getKey().getKey(),
                    AnyTypeKind.USER,
                    userMod.getVirAttrsToRemove(),
                    userMod.getVirAttrsToUpdate());
            tasks.addAll(!propByResVirAttr.isEmpty()
                    ? propagationManager.getUserUpdateTasks(updated, false, null)
                    : Collections.<PropagationTask>emptyList());
        }

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        if (!tasks.isEmpty()) {
            try {
                taskExecutor.execute(tasks, propagationReporter);
            } catch (PropagationException e) {
                LOG.error("Error propagation primary resource", e);
                propagationReporter.onPrimaryResourceFailure(tasks);
            }
        }

        exchange.getOut().setBody(new ImmutablePair<>(
                updated.getResult().getKey().getKey(), propagationReporter.getStatuses()));
    }
}
