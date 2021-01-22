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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Report propagation status after executions.
 */
public interface PropagationReporter {

    /**
     * Report propagation status after executions in case blocking failure (e.g. on priority resources).
     *
     * @param failingResource failing resource name
     * @param taskInfos propagation tasks performed before failure
     */
    void onPriorityResourceFailure(String failingResource, Collection<PropagationTaskInfo> taskInfos);

    /**
     * Report propagation status after executions in case of success or non-blocking failure
     * (e.g. on non-priority resources).
     *
     * @param taskInfo propagation task
     * @param execStatus propagation execution status
     * @param failureReason propagation execution failure message
     * @param fiql FIQL string to match the connector objects into the external resource
     * @param beforeObj retrieved connector object before operation execution
     * @param afterObj retrieved connector object after operation execution
     */
    void onSuccessOrNonPriorityResourceFailures(
            PropagationTaskInfo taskInfo,
            ExecStatus execStatus,
            String failureReason,
            String fiql,
            ConnectorObject beforeObj,
            ConnectorObject afterObj);

    /**
     * Returns the list of propagation statuses.
     *
     * @return the list of propagation statuses
     */
    List<PropagationStatus> getStatuses();
}
