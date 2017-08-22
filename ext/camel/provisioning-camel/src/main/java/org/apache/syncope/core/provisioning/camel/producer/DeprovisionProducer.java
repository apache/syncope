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
package org.apache.syncope.core.provisioning.camel.producer;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;

public class DeprovisionProducer extends AbstractProducer {

    private final UserDAO userDAO;

    private final GroupDAO groupDAO;

    private final AnyObjectDAO anyObjectDAO;

    public DeprovisionProducer(
            final Endpoint endpoint,
            final AnyTypeKind anyTypeKind,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO) {

        super(endpoint, anyTypeKind);
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        String key = exchange.getIn().getBody(String.class);
        List<String> resources = exchange.getProperty("resources", List.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        if (null != getAnyTypeKind()) {
            PropagationByResource propByRes = new PropagationByResource();
            List<PropagationTask> tasks;
            PropagationReporter propagationReporter;
            switch (getAnyTypeKind()) {
                case USER:
                    propByRes.set(ResourceOperation.DELETE, resources);
                    tasks = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.USER,
                            key,
                            propByRes,
                            userDAO.findAllResourceKeys(key).stream().
                                    filter(resource -> !resources.contains(resource)).collect(Collectors.toList()));
                    propagationReporter = getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);
                    exchange.getOut().setBody(propagationReporter.getStatuses());
                    break;

                case GROUP:
                    propByRes.addAll(ResourceOperation.DELETE, resources);
                    tasks = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.GROUP,
                            key,
                            propByRes,
                            groupDAO.findAllResourceKeys(key).stream().
                                    filter(resource -> !resources.contains(resource)).collect(Collectors.toList()));
                    propagationReporter = getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);
                    exchange.getOut().setBody(propagationReporter.getStatuses());
                    break;

                case ANY_OBJECT:
                    propByRes.addAll(ResourceOperation.DELETE, resources);
                    tasks = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.ANY_OBJECT,
                            key,
                            propByRes,
                            anyObjectDAO.findAllResourceKeys(key).stream().
                                    filter(resource -> !resources.contains(resource)).collect(Collectors.toList()));
                    propagationReporter = getPropagationTaskExecutor().execute(tasks, nullPriorityAsync);
                    exchange.getOut().setBody(propagationReporter.getStatuses());
                    break;

                default:
                    break;
            }
        }
    }

}
