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
package org.apache.syncope.core.provisioning.java.pushpull;

import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;

/**
 * Default (empty) implementation of {@link PullActions}.
 */
public abstract class DefaultPullActions implements PullActions {

    @Override
    public void beforeAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
    }

    @Override
    public <A extends AnyTO, P extends AnyPatch> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final A any,
            final P anyMod) throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeDelete(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeAssign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeLink(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeUnassign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeDeprovision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public SyncDelta beforeUnlink(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        return delta;
    }

    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity,
            final ProvisioningReport result)
            throws JobExecutionException {
    }

    @Override
    public IgnoreProvisionException onError(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final Exception e) throws JobExecutionException {

        return null;
    }

    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile)
            throws JobExecutionException {
    }
}
