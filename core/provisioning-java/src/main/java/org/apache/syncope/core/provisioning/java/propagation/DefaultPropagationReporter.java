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
import java.util.List;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultPropagationReporter implements PropagationReporter {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultPropagationReporter.class);

    @Autowired
    protected ConnObjectUtils connObjectUtils;

    protected final List<PropagationStatus> statuses = new ArrayList<>();

    @Override
    public void onSuccessOrSecondaryResourceFailures(final String resource,
            final PropagationTaskExecStatus executionStatus,
            final String failureReason, final ConnectorObject beforeObj, final ConnectorObject afterObj) {

        PropagationStatus propagation = new PropagationStatus();
        propagation.setResource(resource);
        propagation.setStatus(executionStatus);
        propagation.setFailureReason(failureReason);

        if (beforeObj != null) {
            propagation.setBeforeObj(connObjectUtils.getConnObjectTO(beforeObj));
        }

        if (afterObj != null) {
            propagation.setAfterObj(connObjectUtils.getConnObjectTO(afterObj));
        }

        statuses.add(propagation);
    }

    private boolean containsPropagationStatusTO(final String resourceName) {
        for (PropagationStatus status : statuses) {
            if (resourceName.equals(status.getResource())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPrimaryResourceFailure(final List<PropagationTask> tasks) {
        final String failedResource = statuses.get(statuses.size() - 1).getResource();

        LOG.debug("Propagation error: {} primary resource failed to propagate", failedResource);

        for (PropagationTask propagationTask : tasks) {
            if (!containsPropagationStatusTO(propagationTask.getResource().getKey())) {
                PropagationStatus propagationStatusTO = new PropagationStatus();
                propagationStatusTO.setResource(propagationTask.getResource().getKey());
                propagationStatusTO.setStatus(PropagationTaskExecStatus.FAILURE);
                propagationStatusTO.setFailureReason(
                        "Propagation error: " + failedResource + " primary resource failed to propagate.");
                statuses.add(propagationStatusTO);
            }
        }
    }

    @Override
    public List<PropagationStatus> getStatuses() {
        return statuses;
    }
}
