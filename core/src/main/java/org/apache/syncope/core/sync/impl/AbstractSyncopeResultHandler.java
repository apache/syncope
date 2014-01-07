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

import java.util.Collection;
import org.apache.syncope.common.types.ConflictResolutionAction;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.sync.SyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSyncopeResultHandler {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSyncopeResultHandler.class);

    /**
     * Syncing connector.
     */
    protected Connector connector;

    protected Collection<SyncResult> results;

    protected boolean dryRun;

    protected ConflictResolutionAction resAct;

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(final Connector connector) {
        this.connector = connector;
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

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    public ConflictResolutionAction getResAct() {
        return resAct;
    }

    public void setResAct(final ConflictResolutionAction resAct) {
        this.resAct = resAct;
    }
}
