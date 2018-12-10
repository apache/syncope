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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
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
    protected ProvisioningManager<?, ?> getProvisioningManager() {
        return anyObjectProvisioningManager;
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return anyObjectDataBinder.getAnyObjectTO(key);
    }

    @Override
    protected WorkflowResult<? extends AnyUR> update(final AnyUR req) {
        return awfAdapter.update((AnyObjectUR) req);
    }

    @Override
    protected AnyTO doCreate(final AnyTO anyTO, final SyncDelta delta) {
        AnyObjectTO anyObjectTO = AnyObjectTO.class.cast(anyTO);

        Map.Entry<String, List<PropagationStatus>> created = anyObjectProvisioningManager.create(
                anyObjectTO, Collections.singleton(profile.getTask().getResource().getKey()), true);

        return getAnyTO(created.getKey());
    }

    @Override
    protected AnyUR doUpdate(
            final AnyTO before,
            final AnyUR req,
            final SyncDelta delta,
            final ProvisioningReport result) {

        AnyObjectUR anyObjectUR = AnyObjectUR.class.cast(req);

        Pair<AnyObjectUR, List<PropagationStatus>> updated = anyObjectProvisioningManager.update(
                anyObjectUR, Collections.singleton(profile.getTask().getResource().getKey()), true);

        return updated.getLeft();
    }
}
