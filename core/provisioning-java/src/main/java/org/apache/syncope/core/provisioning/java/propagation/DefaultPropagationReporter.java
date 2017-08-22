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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultPropagationReporter implements PropagationReporter {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultPropagationReporter.class);

    @Autowired
    protected ConnObjectUtils connObjectUtils;

    protected final List<PropagationStatus> statuses = new ArrayList<>();

    protected boolean add(final PropagationStatus status) {
        return statuses.stream().anyMatch(item -> item.getResource().equals(status.getResource()))
                ? false
                : statuses.add(status);
    }

    @Override
    public void onSuccessOrNonPriorityResourceFailures(
            final PropagationTask propagationTask,
            final PropagationTaskExecStatus executionStatus,
            final String failureReason,
            final ConnectorObject beforeObj,
            final ConnectorObject afterObj) {

        PropagationStatus status = new PropagationStatus();
        status.setResource(propagationTask.getResource().getKey());
        status.setStatus(executionStatus);
        status.setFailureReason(failureReason);

        if (beforeObj != null) {
            status.setBeforeObj(connObjectUtils.getConnObjectTO(beforeObj));
        }

        if (afterObj != null) {
            status.setAfterObj(connObjectUtils.getConnObjectTO(afterObj));
        }

        add(status);
    }

    @Override
    public void onPriorityResourceFailure(final String failingResource, final Collection<PropagationTask> tasks) {
        LOG.debug("Propagation error: {} priority resource failed to propagate", failingResource);

        Optional<PropagationTask> propagationTask = tasks.stream().
                filter(task -> task.getResource().getKey().equals(failingResource)).findFirst();

        if (propagationTask.isPresent()) {
            PropagationStatus status = new PropagationStatus();
            status.setResource(propagationTask.get().getResource().getKey());
            status.setStatus(PropagationTaskExecStatus.FAILURE);
            status.setFailureReason(
                    "Propagation error: " + failingResource + " priority resource failed to propagate.");
            add(status);
        } else {
            LOG.error("Could not find {} for {}", PropagationTask.class.getName(), failingResource);
        }
    }

    @Override
    public List<PropagationStatus> getStatuses() {
        return Collections.unmodifiableList(statuses);
    }
}
