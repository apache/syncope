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

import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.identityconnectors.framework.common.objects.ConnectorObject;

public interface PropagationActions {

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param taskInfo propagation task
     * @param orgUnit Realm provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(Optional<PropagationTaskInfo> taskInfo, OrgUnit orgUnit) {
        return Set.of();
    }

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param taskInfo propagation task
     * @param provision Any provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(Optional<PropagationTaskInfo> taskInfo, Provision provision) {
        return Set.of();
    }

    /**
     * Executes logic before actual propagation.
     *
     * @param taskInfo propagation task
     */
    default void before(PropagationTaskInfo taskInfo) {
        // do nothing
    }

    /**
     * Executes logic in case of propagation error.
     * This method can throw {@link org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException} to
     * ignore the reported error and continue.
     *
     * @param taskInfo propagation task
     * @param execution execution result
     * @param error propagation error
     */
    default void onError(PropagationTaskInfo taskInfo, TaskExec<PropagationTask> execution, Exception error) {
        // do nothing
    }

    /**
     * Executes logic after actual propagation.
     *
     * @param taskInfo propagation task
     * @param execution execution result
     * @param afterObj connector object read after propagation
     */
    default void after(PropagationTaskInfo taskInfo, TaskExec<PropagationTask> execution, ConnectorObject afterObj) {
        // do nothing
    }
}
