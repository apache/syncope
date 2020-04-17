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
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.identityconnectors.framework.common.objects.ConnectorObject;

public interface PropagationActions {

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param task propagation task
     * @param orgUnit Realm provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(Optional<PropagationTask> task, OrgUnit orgUnit) {
        return Set.of();
    }

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param task propagation task
     * @param provision Any provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(Optional<PropagationTask> task, Provision provision) {
        return Set.of();
    }

    /**
     * Executes logic before actual propagation.
     *
     * @param task propagation task
     * @param beforeObj connector object read before propagation
     */
    default void before(PropagationTask task, ConnectorObject beforeObj) {
        // do nothing
    }

    /**
     * Executes logic in case of propagation error.
     * This method can throw {@link org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException} to
     * ignore the reported error and continue.
     *
     * @param task propagation task
     * @param execution execution result
     * @param error propagation error
     */
    default void onError(PropagationTask task, TaskExec execution, Exception error) {
        // do nothing
    }

    /**
     * Executes logic after actual propagation.
     *
     * @param task propagation task
     * @param execution execution result
     * @param afterObj connector object read after propagation
     */
    default void after(PropagationTask task, TaskExec execution, ConnectorObject afterObj) {
        // do nothing
    }
}
