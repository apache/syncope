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
package org.apache.syncope.core.provisioning.java.sync;

import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.quartz.JobExecutionException;

/**
 * Default (empty) implementation of PushActions.
 */
public abstract class DefaultPushActions implements PushActions {

    @Override
    public void beforeAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
    }

    @Override
    public <T extends Any<?, ?, ?>> T beforeAssign(final ProvisioningProfile<?, ?> profile, final T any)
            throws JobExecutionException {

        return any;
    }

    @Override
    public <T extends Any<?, ?, ?>> T beforeProvision(final ProvisioningProfile<?, ?> profile, final T any)
            throws JobExecutionException {

        return any;
    }

    @Override
    public <T extends Any<?, ?, ?>> T beforeLink(final ProvisioningProfile<?, ?> profile, final T any)
            throws JobExecutionException {

        return any;
    }

    @Override
    public <T extends Any<?, ?, ?>> T beforeUnassign(final ProvisioningProfile<?, ?> profile, final T any)
            throws JobExecutionException {

        return any;
    }

    @Override
    public <T extends Any<?, ?, ?>> T beforeDeprovision(final ProvisioningProfile<?, ?> profile, final T any)
            throws JobExecutionException {

        return any;
    }

    @Override
    public <T extends Any<?, ?, ?>> T beforeUnlink(final ProvisioningProfile<?, ?> profile, final T any)
            throws JobExecutionException {

        return any;
    }

    @Override
    public <T extends Any<?, ?, ?>> void onError(
            final ProvisioningProfile<?, ?> profile, final T any, final ProvisioningResult result,
            final Exception error) throws JobExecutionException {

        // do nothing
    }

    @Override
    public <T extends Any<?, ?, ?>> void after(
            final ProvisioningProfile<?, ?> profile, final T any, final ProvisioningResult result)
            throws JobExecutionException {

        // do nothing
    }

    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile)
            throws JobExecutionException {

        // do nothing
    }
}
