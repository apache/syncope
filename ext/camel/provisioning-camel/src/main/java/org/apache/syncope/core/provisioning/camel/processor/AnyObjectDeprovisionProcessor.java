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
import java.util.HashSet;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnyObjectDeprovisionProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(UserDeprovisionProcessor.class);

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) {
        Long anyObjectKey = exchange.getIn().getBody(Long.class);
        List<String> resources = exchange.getProperty("resources", List.class);

        AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);

        Collection<String> noPropResourceNames = CollectionUtils.removeAll(anyObject.getResourceNames(), resources);

        List<PropagationTask> tasks =
                propagationManager.getAnyObjectDeleteTasks(anyObjectKey, new HashSet<>(resources), noPropResourceNames);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        exchange.getOut().setBody(propagationReporter.getStatuses());
    }

}
