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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroupDeleteProcessor implements Processor {

    @Autowired
    protected GroupWorkflowAdapter gwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected GroupDAO groupDAO;

    @Override
    public void process(final Exchange exchange) throws Exception {
        Long key = exchange.getIn().getBody(Long.class);
        @SuppressWarnings("unchecked")
        Set<String> excludedResources = exchange.getProperty("excludedResources", Set.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        List<PropagationTask> tasks = new ArrayList<>();

        // Generate propagation tasks for deleting users from group resources, if they are on those resources only
        // because of the reason being deleted (see SYNCOPE-357)
        for (Map.Entry<Long, PropagationByResource> entry
                : groupDAO.findUsersWithTransitiveResources(key).entrySet()) {

            tasks.addAll(propagationManager.getDeleteTasks(
                    AnyTypeKind.USER,
                    entry.getKey(),
                    entry.getValue(),
                    excludedResources));
        }
        for (Map.Entry<Long, PropagationByResource> entry
                : groupDAO.findAnyObjectsWithTransitiveResources(key).entrySet()) {

            tasks.addAll(propagationManager.getDeleteTasks(
                    AnyTypeKind.ANY_OBJECT,
                    entry.getKey(),
                    entry.getValue(),
                    excludedResources));
        }

        // Generate propagation tasks for deleting this group from resources
        tasks.addAll(propagationManager.getDeleteTasks(
                AnyTypeKind.GROUP,
                key,
                null,
                null));

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        exchange.setProperty("statuses", propagationReporter.getStatuses());
    }

}
