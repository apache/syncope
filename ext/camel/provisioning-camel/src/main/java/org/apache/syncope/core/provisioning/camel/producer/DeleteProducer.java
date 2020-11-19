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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

public class DeleteProducer extends AbstractProducer {

    private final UserDAO userDAO;

    private final GroupDataBinder groupDataBinder;

    public DeleteProducer(
            final Endpoint endpoint,
            final AnyTypeKind anyTypeKind,
            final UserDAO userDao,
            final GroupDataBinder groupDataBinder) {

        super(endpoint, anyTypeKind);
        this.userDAO = userDao;
        this.groupDataBinder = groupDataBinder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) throws Exception {
        String key = exchange.getIn().getBody(String.class);
        Set<String> excludedResources = exchange.getProperty("excludedResources", Set.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        if (null != getAnyTypeKind()) {
            List<PropagationTaskInfo> taskInfos;
            PropagationReporter reporter;
            switch (getAnyTypeKind()) {
                case USER:
                    PropagationByResource<String> propByRes = new PropagationByResource<>();
                    propByRes.set(ResourceOperation.DELETE, userDAO.findAllResourceKeys(key));

                    PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
                    userDAO.findLinkedAccounts(key).forEach(account -> propByLinkedAccount.add(
                            ResourceOperation.DELETE,
                            Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

                    // Note here that we can only notify about "delete", not any other
                    // task defined in workflow process definition: this because this
                    // information could only be available after uwfAdapter.delete(), which
                    // will also effectively remove user from db, thus making virtually
                    // impossible by NotificationManager to fetch required user information
                    taskInfos = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.USER,
                            key,
                            propByRes,
                            propByLinkedAccount,
                            excludedResources);
                    reporter = getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());
                    exchange.setProperty("statuses", reporter.getStatuses());
                    break;

                case GROUP:
                    taskInfos = new ArrayList<>();
                    // Generate propagation tasks for deleting users from group resources, if they are on those
                    // resources only because of the reason being deleted (see SYNCOPE-357)
                    groupDataBinder.findUsersWithTransitiveResources(key).
                            forEach((anyKey, anyPropByRes) -> taskInfos.addAll(
                            getPropagationManager().getDeleteTasks(
                                    AnyTypeKind.USER,
                                    anyKey,
                                    anyPropByRes,
                                    null,
                                    excludedResources)));
                    groupDataBinder.findAnyObjectsWithTransitiveResources(key).
                            forEach((anyKey, anyPropByRes) -> {
                                taskInfos.addAll(getPropagationManager().getDeleteTasks(
                                        AnyTypeKind.ANY_OBJECT,
                                        anyKey,
                                        anyPropByRes,
                                        null,
                                        excludedResources));
                            });
                    // Generate propagation tasks for deleting this group from resources
                    taskInfos.addAll(getPropagationManager().getDeleteTasks(
                            AnyTypeKind.GROUP,
                            key,
                            null,
                            null,
                            null));
                    reporter = getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());
                    exchange.setProperty("statuses", reporter.getStatuses());
                    break;

                case ANY_OBJECT:
                    taskInfos = getPropagationManager().getDeleteTasks(
                            AnyTypeKind.ANY_OBJECT,
                            key,
                            null,
                            null,
                            excludedResources);
                    reporter = getPropagationTaskExecutor().execute(taskInfos, nullPriorityAsync, getExecutor());
                    exchange.setProperty("statuses", reporter.getStatuses());
                    break;

                default:
                    break;
            }
        }
    }
}
