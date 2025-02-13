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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultAnyObjectPullResultHandler extends AbstractPullResultHandler implements AnyObjectPullResultHandler {

    @Autowired
    private AnyObjectProvisioningManager anyObjectProvisioningManager;

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);
    }

    @Override
    protected String getName(final AnyTO anyTO) {
        return AnyObjectTO.class.cast(anyTO).getName();
    }

    @Override
    protected String getName(final AnyCR anyCR) {
        return AnyObjectCR.class.cast(anyCR).getName();
    }

    @Override
    protected ProvisioningManager<?, ?> getProvisioningManager() {
        return anyObjectProvisioningManager;
    }

    @Override
    protected AnyTO getAnyTO(final Any any) {
        return anyObjectDataBinder.getAnyObjectTO((AnyObject) any, true);
    }

    @Override
    protected WorkflowResult<? extends AnyUR> update(final AnyUR req) {
        return awfAdapter.update((AnyObjectUR) req, profile.getExecutor(), profile.getContext());
    }

    @Override
    protected AnyTO doCreate(final AnyCR anyCR, final SyncDelta delta) {
        AnyObjectCR anyObjectCR = AnyObjectCR.class.cast(anyCR);

        Map.Entry<String, List<PropagationStatus>> created = anyObjectProvisioningManager.create(
                anyObjectCR,
                Set.of(profile.getTask().getResource().getKey()),
                true,
                profile.getExecutor(),
                profile.getContext());

        return anyObjectDataBinder.getAnyObjectTO(created.getKey());
    }

    @Override
    protected AnyUR doUpdate(
            final AnyTO before,
            final AnyUR req,
            final SyncDelta delta,
            final ProvisioningReport result) {

        AnyObjectUR anyObjectUR = AnyObjectUR.class.cast(req);

        Pair<AnyObjectUR, List<PropagationStatus>> updated = anyObjectProvisioningManager.update(
                anyObjectUR,
                Set.of(profile.getTask().getResource().getKey()),
                true,
                profile.getExecutor(),
                profile.getContext());

        createRemediationIfNeeded(req, delta, result);

        return updated.getLeft();
    }
}
