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

import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Handle propagation executions.
 */
public interface PropagationHandler {

    /**
     *
     * Handle propagation executions.
     *
     * @param resourceName resource name.
     * @param execStatus propagation execution status.
     * @param failureReason propagation execution failure message.
     * @param beforeObj retrieved connector object before operation execution.
     * @param afterObj retrieved connector object after operation execution.
     */
    void handle(String resourceName, PropagationTaskExecStatus execStatus,
            String failureReason, ConnectorObject beforeObj, ConnectorObject afterObj);
}
