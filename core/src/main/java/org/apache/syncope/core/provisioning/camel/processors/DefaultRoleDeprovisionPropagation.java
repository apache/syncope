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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultRoleDeprovisionPropagation implements Processor{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserDeprovisionPropagation.class);
    
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    @Autowired
    protected RoleDataBinder binder;
    
    @Override
    public void process(Exchange exchange){
        
        Long roleId = exchange.getIn().getBody(Long.class);
        List<String> resources = exchange.getProperty("resources", List.class);
        
        final SyncopeRole role = binder.getRoleFromId(roleId);
        
        final Set<String> noPropResourceName = role.getResourceNames();
        noPropResourceName.removeAll(resources);
        
        final List<PropagationTask> tasks = propagationManager.getRoleDeleteTaskIds(roleId, new HashSet<String>(resources), noPropResourceName);
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        
        exchange.getOut().setBody(propagationReporter.getStatuses());
    }    
    
}
