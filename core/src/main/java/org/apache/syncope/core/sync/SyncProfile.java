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
package org.apache.syncope.core.sync;

import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.beans.AbstractSyncTask;
import org.apache.syncope.core.propagation.Connector;

public class SyncProfile<T extends AbstractSyncTask, A extends AbstractSyncActions<?>> {

    /**
     * Syncing connector.
     */
    private Connector connector;

    private Collection<SyncResult> results;

    private boolean dryRun;

    private ConflictResolutionAction resAct;

    private List<A> actions;

    private T syncTask;

    public SyncProfile(final Connector connector, final T syncTask) {
        this.connector = connector;
        this.syncTask = syncTask;
    }

    public Connector getConnector() {
        return connector;
    }

    public Collection<SyncResult> getResults() {
        return results;
    }

    public void setResults(final Collection<SyncResult> results) {
        this.results = results;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
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

    public void setActions(
            List<A> actions) {
        this.actions = actions;
    }

    public T getSyncTask() {
        return syncTask;
    }
}
