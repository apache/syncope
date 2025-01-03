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
package org.apache.syncope.core.provisioning.api.pushpull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.Connector;

public class ProvisioningProfile<T extends ProvisioningTask<?>, A extends ProvisioningActions> {

    private final Connector connector;

    private final TaskType taskType;

    private final T task;

    private final ConflictResolutionAction conflictResolutionAction;

    private final String executor;

    private final boolean dryRun;

    private final List<A> actions;

    private final List<ProvisioningReport> results = new CopyOnWriteArrayList<>();

    public ProvisioningProfile(
            final Connector connector,
            final TaskType taskType,
            final T task,
            final ConflictResolutionAction conflictResolutionAction,
            final List<A> actions,
            final String executor,
            final boolean dryRun) {

        this.connector = connector;
        this.taskType = taskType;
        this.task = task;
        this.conflictResolutionAction = conflictResolutionAction;
        this.actions = actions;
        this.executor = executor;
        this.dryRun = dryRun;
    }

    public Connector getConnector() {
        return connector;
    }

    public T getTask() {
        return task;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction;
    }

    public String getExecutor() {
        return executor;
    }

    public List<A> getActions() {
        return actions;
    }

    public List<ProvisioningReport> getResults() {
        return results;
    }

    public String getContext() {
        return taskType + " Task " + task.getKey() + " '" + task.getName() + "'";
    }
}
