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

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserWFSuspendPropagation implements Processor{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserWFSuspendPropagation.class);
    
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Override
    public void process(Exchange exchange){
                 
        WorkflowResult<Long> updated = (WorkflowResult) exchange.getIn().getBody();            
        Boolean suspend = exchange.getProperty("suspend", Boolean.class);

        if (suspend) {
            UserMod userMod = new UserMod();
            userMod.setId(updated.getResult());

            final List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                            new SimpleEntry<UserMod, Boolean>(userMod, Boolean.FALSE),
                            updated.getPropByRes(), updated.getPerformedTasks()));

            taskExecutor.execute(tasks);
        }
    }
    
}
