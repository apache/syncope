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
package org.apache.syncope.core.sync.impl;

import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.sync.SyncActions;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;

/**
 * Abstract class introduced to facilitate sync handler extension/override.
 */
public abstract class AbstractSyncopeSyncResultHandler extends AbstractSyncopeResultHandler
        implements SyncResultsHandler {

    /**
     * SyncJob actions.
     */
    protected SyncActions actions;

    protected SyncTask syncTask;

    public SyncActions getActions() {
        return actions;
    }

    public void setActions(final SyncActions actions) {
        this.actions = actions;
    }

    public SyncTask getSyncTask() {
        return syncTask;
    }

    public void setSyncTask(final SyncTask syncTask) {
        this.syncTask = syncTask;
    }
}
