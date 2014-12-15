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

import java.util.List;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class DefaultUserDeletePropagation implements Processor{
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserDeletePropagation.class);
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Override
    public void process(Exchange exchange) throws Exception {
        
        Long userId = (Long) exchange.getIn().getBody();       
        LOG.info("UserId {} ", userId);
        
        Set<String> excludedResource = exchange.getProperty("excludedResources", Set.class);            
        
        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after uwfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(userId,excludedResource);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        
        exchange.setProperty("statuses", propagationReporter.getStatuses());
    }
    
}
