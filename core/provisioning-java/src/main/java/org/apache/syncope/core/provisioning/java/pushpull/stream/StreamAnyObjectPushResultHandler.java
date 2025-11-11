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
package org.apache.syncope.core.provisioning.java.pushpull.stream;

import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationReporter;
import org.apache.syncope.core.provisioning.java.pushpull.DefaultAnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class StreamAnyObjectPushResultHandler extends DefaultAnyObjectPushResultHandler {

    @Autowired
    private DerAttrHandler derAttrHandler;

    @Override
    protected void provision(final Any any, final Boolean enabled, final ProvisioningReport result) {
        Provision provision = profile.getTask().getResource().getProvisions().getFirst();

        Stream<Item> items = MappingUtils.getPropagationItems(provision.getMapping().getItems().stream());

        MappingManager.PreparedAttrs preparedAttrs = mappingManager.prepareAttrsFromAny(
                any, null, false, enabled, profile.getTask().getResource(), provision);

        PropagationTaskInfo propagationTask = propagationManager.newTask(
                derAttrHandler,
                any,
                profile.getTask().getResource(),
                ResourceOperation.CREATE,
                provision,
                items,
                preparedAttrs);
        propagationTask.setConnector(profile.getConnector());
        LOG.debug("PropagationTask created: {}", propagationTask);

        PropagationReporter reporter = new DefaultPropagationReporter();
        taskExecutor.execute(propagationTask, reporter, profile.getExecutor());
        reportPropagation(result, reporter);
    }
}
