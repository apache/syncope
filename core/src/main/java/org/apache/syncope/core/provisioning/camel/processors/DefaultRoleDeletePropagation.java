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

package org.apache.syncope.core.provisioning.camel.processors;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultRoleDeletePropagation implements Processor{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRoleDeletePropagation.class);
    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;    
    @Autowired
    protected RoleDAO roleDAO;
    @Autowired
    protected RoleDataBinder binder;
    
    @Override
    public void process(Exchange exchange) throws Exception {
        
        Long subjectId = exchange.getIn().getBody(Long.class);
        
        final List<SyncopeRole> toBeDeprovisioned = new ArrayList<SyncopeRole>();

        final SyncopeRole syncopeRole = roleDAO.find(subjectId);

        if (syncopeRole != null) {
            toBeDeprovisioned.add(syncopeRole);

            final List<SyncopeRole> descendants = roleDAO.findDescendants(toBeDeprovisioned.get(0));
            if (descendants != null) {
                toBeDeprovisioned.addAll(descendants);
            }
        }

        final List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        for (SyncopeRole role : toBeDeprovisioned) {
            // Generate propagation tasks for deleting users from role resources, if they are on those resources only
            // because of the reason being deleted (see SYNCOPE-357)
            for (WorkflowResult<Long> wfResult : binder.getUsersOnResourcesOnlyBecauseOfRole(role.getId())) {
                tasks.addAll(propagationManager.getUserDeleteTaskIds(wfResult));
            }

            // Generate propagation tasks for deleting this role from resources
            tasks.addAll(propagationManager.getRoleDeleteTaskIds(role.getId()));
        }

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }      
        
        exchange.setProperty("statuses", propagationReporter.getStatuses());
    }
    
    
    
}
