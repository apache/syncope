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

import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.quartz.JobExecutionException;

/**
 * Default (empty) implementation of PushActions.
 */
public abstract class DefaultPushActions implements PushActions {

    @Override
    public void beforeAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
    }

    @Override
    public Entity beforeAssign(final ProvisioningProfile<?, ?> profile, final Entity entity)
            throws JobExecutionException {

        return entity;
    }

    @Override
    public Entity beforeProvision(final ProvisioningProfile<?, ?> profile, final Entity entity)
            throws JobExecutionException {

        return entity;
    }

    @Override
    public Entity beforeLink(final ProvisioningProfile<?, ?> profile, final Entity entity)
            throws JobExecutionException {

        return entity;
    }

    @Override
    public Entity beforeUnassign(final ProvisioningProfile<?, ?> profile, final Entity entity)
            throws JobExecutionException {

        return entity;
    }

    @Override
    public Entity beforeDeprovision(final ProvisioningProfile<?, ?> profile, final Entity entity)
            throws JobExecutionException {

        return entity;
    }

    @Override
    public Entity beforeUnlink(final ProvisioningProfile<?, ?> profile, final Entity entity)
            throws JobExecutionException {

        return entity;
    }

    @Override
    public void onError(
            final ProvisioningProfile<?, ?> profile, final Entity entity, final ProvisioningReport result,
            final Exception error) throws JobExecutionException {

        // do nothing
    }

    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile, final Entity entity, final ProvisioningReport result)
            throws JobExecutionException {

        // do nothing
    }

    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile)
            throws JobExecutionException {

        // do nothing
    }
}
