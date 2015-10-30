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
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserProvisionProcessor implements Processor {

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected UserDAO userDAO;

    @Override
    public void process(final Exchange exchange) {
        Long key = exchange.getIn().getBody(Long.class);
        Boolean changePwd = exchange.getProperty("changePwd", Boolean.class);
        String password = exchange.getProperty("password", String.class);
        @SuppressWarnings("unchecked")
        List<String> resources = exchange.getProperty("resources", List.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(key);
        userPatch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        if (changePwd) {
            userPatch.setPassword(
                    new PasswordPatch.Builder().onSyncope(true).value(password).resources(resources).build());
        }

        PropagationByResource propByRes = new PropagationByResource();
        for (String resource : resources) {
            propByRes.add(ResourceOperation.UPDATE, resource);
        }

        WorkflowResult<Pair<UserPatch, Boolean>> wfResult = new WorkflowResult<Pair<UserPatch, Boolean>>(
                ImmutablePair.of(userPatch, (Boolean) null), propByRes, "update");

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(wfResult, changePwd, null);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        exchange.getOut().setBody(propagationReporter.getStatuses());
    }

}
