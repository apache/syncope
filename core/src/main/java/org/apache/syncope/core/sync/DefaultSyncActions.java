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

import java.util.List;
import org.apache.syncope.client.mod.AbstractAttributableMod;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.core.persistence.beans.SyncActions;
import org.apache.syncope.core.persistence.beans.SyncResult;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Default (empty) implementation of SyncActions.
 */
public class DefaultSyncActions implements SyncActions {

    @Override
    public void beforeAll(final SyncTask task) throws JobExecutionException {
    }

    @Override
    public <T extends AbstractAttributableTO> SyncDelta beforeCreate(final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractAttributableTO, K extends AbstractAttributableMod> SyncDelta beforeUpdate(
            final SyncDelta delta, final T subject, final K subjectMod) throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractAttributableTO> SyncDelta beforeDelete(final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractAttributableTO> void after(final SyncDelta delta, final T subject,
            final SyncResult result) throws JobExecutionException {
    }

    @Override
    public void afterAll(final SyncTask task, final List<SyncResult> results) throws JobExecutionException {
    }
}
