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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

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
            PropagationByResource<String> propByRes = new PropagationByResource<>();
            List<PropagationTaskInfo> taskInfos;
            PropagationReporter propagationReporter;
            switch (getAnyTypeKind()) {
                case USER:
                    propByRes.set(ResourceOperation.DELETE, resources);

                    PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
                    userDAO.findLinkedAccounts(key).stream().
                            filter(account -> resources.contains(account.getResource().getKey())).
                            forEach(account -> propByLinkedAccount.add(
                            ResourceOperation.DELETE,
                            Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

                    taskInfos = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.USER,
                            key,
                            propByRes,
                            propByLinkedAccount,
                            userDAO.findAllResourceKeys(key).stream().
                                    filter(resource -> !resources.contains(resource)).collect(Collectors.toList()));
                    propagationReporter =
                        getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());
                    exchange.getMessage().setBody(propagationReporter.getStatuses());
                    break;

                case GROUP:
                    propByRes.addAll(ResourceOperation.DELETE, resources);
                    taskInfos = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.GROUP,
                            key,
                            propByRes,
                            null,
                            groupDAO.findAllResourceKeys(key).stream().
                                    filter(resource -> !resources.contains(resource)).collect(Collectors.toList()));
                    propagationReporter =
                        getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());
                    exchange.getMessage().setBody(propagationReporter.getStatuses());
                    break;

                case ANY_OBJECT:
                    propByRes.addAll(ResourceOperation.DELETE, resources);
                    taskInfos = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.ANY_OBJECT,
                            key,
                            propByRes,
                            null,
                            anyObjectDAO.findAllResourceKeys(key).stream().
                                    filter(resource -> !resources.contains(resource)).collect(Collectors.toList()));
                    propagationReporter =
                        getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());
                    exchange.getMessage().setBody(propagationReporter.getStatuses());
                    break;

                default:
                    break;
            }
        }
    }
}
