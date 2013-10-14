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
package org.apache.syncope.core.propagation;

import java.util.List;
import org.apache.syncope.common.to.PropagationStatusTO;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Report propagation status after executions.
 */
public interface PropagationReporter {

    /**
     * Report propagation status after executions in case of success or non-blocking failure
     * (e.g. on secondary resources).
     *
     * @param resourceName resource name.
     * @param execStatus propagation execution status.
     * @param failureReason propagation execution failure message.
     * @param beforeObj retrieved connector object before operation execution.
     * @param afterObj retrieved connector object after operation execution.
     */
    void onSuccessOrSecondaryResourceFailures(String resourceName, PropagationTaskExecStatus execStatus,
            String failureReason, ConnectorObject beforeObj, ConnectorObject afterObj);

    /**
     * Report propagation status after executions in case blocking failure (e.g. on primary resources).
     * 
     * @param tasks propagation tasks performed before failure
     */
    void onPrimaryResourceFailure(List<PropagationTask> tasks);

    /**
     * Returns the list of propagation statuses.
     *
     * @return the list of propagation statuses
     */
    List<PropagationStatusTO> getStatuses();
}
