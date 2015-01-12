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
package org.apache.syncope.server.provisioning.java.sync;

import java.util.List;
import org.apache.syncope.common.lib.mod.AbstractSubjectMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.server.provisioning.api.sync.SyncActions;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Default (empty) implementation of SyncActions.
 */
public abstract class DefaultSyncActions implements SyncActions {

    @Override
    public void beforeAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
    }

    @Override
    public <T extends AbstractSubjectTO, K extends AbstractSubjectMod> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final K subjectMod) throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeDelete(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeAssign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeLink(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeUnassign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeDeprovision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> SyncDelta beforeUnlink(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractSubjectTO> void after(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final T subject, final ProvisioningResult result)
            throws JobExecutionException {
    }

    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile, final List<ProvisioningResult> results)
            throws JobExecutionException {
    }
}
