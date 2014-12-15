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

import java.util.Map;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class UserStatusOnSync implements Processor{
    
    private static final Logger LOG = LoggerFactory.getLogger(UserStatusOnSync.class);
    
    @Autowired
    protected UserDAO userDAO;
    @Autowired
    protected UserWorkflowAdapter uwfAdapter;
    
    @Override
    public void process(Exchange exchange){
        
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = (WorkflowResult) exchange.getIn().getBody();                    
          
        Boolean enabled = exchange.getProperty("enabled", Boolean.class);
        Long id = exchange.getProperty("id", Long.class);
                
        if (enabled != null) {
             SyncopeUser user = userDAO.find(id);

             WorkflowResult<Long> enableUpdate = null;
             if (user.isSuspended() == null) {
                 enableUpdate = uwfAdapter.activate(id, null);
             } else if (enabled && user.isSuspended()) {
                 enableUpdate = uwfAdapter.reactivate(id);
             } else if (!enabled && !user.isSuspended()) {
                 enableUpdate = uwfAdapter.suspend(id);
             }

             if (enableUpdate != null) {
                 if (enableUpdate.getPropByRes() != null) {
                     updated.getPropByRes().merge(enableUpdate.getPropByRes());
                     updated.getPropByRes().purge();
                 }
                 updated.getPerformedTasks().addAll(enableUpdate.getPerformedTasks());
             }
       }
    }
}
