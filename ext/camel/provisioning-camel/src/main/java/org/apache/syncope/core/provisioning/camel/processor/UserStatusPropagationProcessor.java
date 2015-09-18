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

import java.util.Collection;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
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
public class UserStatusPropagationProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(UserStatusPropagationProcessor.class);

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected UserDAO userDAO;

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) {
        WorkflowResult<Long> updated = (WorkflowResult) exchange.getIn().getBody();

        Long key = exchange.getProperty("userKey", Long.class);
        StatusPatch statusPatch = exchange.getProperty("statusPatch", StatusPatch.class);

        Collection<String> noPropResourceNames = CollectionUtils.removeAll(
                userDAO.findAllResourceNames(userDAO.find(key)), statusPatch.getResources());

        List<PropagationTask> tasks = propagationManager.getUpdateTasks(
                AnyTypeKind.USER,
                statusPatch.getKey(),
                false,
                statusPatch.getType() != StatusPatchType.SUSPEND,
                null,
                null,
                noPropResourceNames);
        PropagationReporter propReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }

        exchange.getOut().setBody(new ImmutablePair<>(updated.getResult(), propReporter.getStatuses()));
    }
}
