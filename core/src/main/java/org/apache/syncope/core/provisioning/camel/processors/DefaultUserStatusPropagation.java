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

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserStatusPropagation implements Processor{
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserStatusPropagation.class);
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Override
    public void process(Exchange exchange){
        
        WorkflowResult<Long> updated = (WorkflowResult) exchange.getIn().getBody();
        
        SyncopeUser user = exchange.getProperty("user", SyncopeUser.class);
        StatusMod statusMod = exchange.getProperty("statusMod", StatusMod.class);
        
        Set<String> resourcesToBeExcluded = new HashSet<String>(user.getResourceNames());
        resourcesToBeExcluded.removeAll(statusMod.getResourceNames());

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                user, statusMod.getType() != StatusMod.ModType.SUSPEND, resourcesToBeExcluded);
        PropagationReporter propReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }
        
        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(updated.getResult(), propReporter.getStatuses());
        exchange.getOut().setBody(result); 
    }    
}
