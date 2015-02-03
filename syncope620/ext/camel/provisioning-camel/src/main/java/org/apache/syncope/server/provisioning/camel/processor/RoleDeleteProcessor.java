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
package org.apache.syncope.server.provisioning.camel.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.server.misc.spring.ApplicationContextProvider;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.server.provisioning.api.WorkflowResult;
import org.apache.syncope.server.provisioning.api.propagation.PropagationException;
import org.apache.syncope.server.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.server.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.server.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.server.workflow.api.RoleWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleDeleteProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(RoleDeleteProcessor.class);

    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected RoleDAO roleDAO;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final List<Role> toBeDeprovisioned = new ArrayList<>();

        Long subjectKey = exchange.getIn().getBody(Long.class);
        final Role syncopeRole = roleDAO.find(subjectKey);

        if (syncopeRole != null) {
            toBeDeprovisioned.add(syncopeRole);

            final List<Role> descendants = roleDAO.findDescendants(toBeDeprovisioned.get(0));
            if (descendants != null) {
                toBeDeprovisioned.addAll(descendants);
            }
        }

        final List<PropagationTask> tasks = new ArrayList<>();

        for (Role role : toBeDeprovisioned) {
            // Generate propagation tasks for deleting users from role resources, if they are on those resources only
            // because of the reason being deleted (see SYNCOPE-357)
            for (Map.Entry<Long, PropagationByResource> entry : roleDAO.findUsersWithIndirectResources(role.
                    getKey()).entrySet()) {

                WorkflowResult<Long> wfResult =
                        new WorkflowResult<>(entry.getKey(), entry.getValue(), Collections.<String>emptySet());
                tasks.addAll(propagationManager.getUserDeleteTaskIds(wfResult));
            }

            // Generate propagation tasks for deleting this role from resources
            tasks.addAll(propagationManager.getRoleDeleteTaskIds(role.getKey()));
        }

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        exchange.setProperty("statuses", propagationReporter.getStatuses());
    }

}
