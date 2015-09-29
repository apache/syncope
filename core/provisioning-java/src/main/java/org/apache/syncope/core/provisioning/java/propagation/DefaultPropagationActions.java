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

import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Default (empty) implementation of {@link PropagationActions}.
 */
public abstract class DefaultPropagationActions implements PropagationActions {

    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        // do nothing
    }

    @Override
    public void onError(final PropagationTask task, final TaskExec execution, final Exception error) {
        // do nothing
    }

    @Override
    public void after(final PropagationTask task, final TaskExec execution, final ConnectorObject afterObj) {
        // do nothing
    }
}
