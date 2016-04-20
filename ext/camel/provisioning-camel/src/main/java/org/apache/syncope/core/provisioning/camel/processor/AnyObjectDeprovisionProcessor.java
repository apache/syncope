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

import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnyObjectDeprovisionProcessor implements Processor {

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) {
        String key = exchange.getIn().getBody(String.class);
        List<String> resources = exchange.getProperty("resources", List.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.DELETE, resources);

        List<PropagationTask> tasks = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                propByRes,
                CollectionUtils.removeAll(anyObjectDAO.findAllResourceNames(anyObjectDAO.authFind(key)), resources));
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        exchange.getOut().setBody(propagationReporter.getStatuses());
    }

}
