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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.Connector;

public class ProvisioningProfile<T extends ProvisioningTask, A extends ProvisioningActions> {

    private final Connector connector;

    private final T task;

    private final List<ProvisioningReport> results = new ArrayList<>();

    private boolean dryRun;

    private ConflictResolutionAction resAct;

    private final List<A> actions = new ArrayList<>();

    public ProvisioningProfile(final Connector connector, final T task) {
        this.connector = connector;
        this.task = task;
    }

    public Connector getConnector() {
        return connector;
    }

    public T getTask() {
        return task;
    }

    public Collection<ProvisioningReport> getResults() {
        return results;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    public ConflictResolutionAction getResAct() {
        return resAct;
    }

    public void setResAct(final ConflictResolutionAction resAct) {
        this.resAct = resAct;
    }

    public List<A> getActions() {
        return actions;
    }
}
