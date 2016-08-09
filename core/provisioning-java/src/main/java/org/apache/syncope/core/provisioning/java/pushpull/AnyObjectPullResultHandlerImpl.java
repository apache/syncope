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
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;

public class AnyObjectPullResultHandlerImpl extends AbstractPullResultHandler implements AnyObjectPullResultHandler {

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
    protected Any<?> getAny(final String key) {
        try {
            return anyObjectDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving anyObject {}", key, e);
            return null;
        }
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return anyObjectDataBinder.getAnyObjectTO(key);
    }

    @Override
    protected AnyPatch newPatch(final String key) {
        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    protected WorkflowResult<String> update(final AnyPatch patch) {
        return awfAdapter.update((AnyObjectPatch) patch);
    }

    @Override
    protected AnyTO doCreate(final AnyTO anyTO, final SyncDelta delta, final ProvisioningReport result) {
        AnyObjectTO anyObjectTO = AnyObjectTO.class.cast(anyTO);

        Map.Entry<String, List<PropagationStatus>> created = anyObjectProvisioningManager.create(
                anyObjectTO, Collections.singleton(profile.getTask().getResource().getKey()), true);

        result.setKey(created.getKey());
        result.setName(getName(anyTO));

        return getAnyTO(created.getKey());
    }

    @Override
    protected AnyTO doUpdate(
            final AnyTO before,
            final AnyPatch anyPatch,
            final SyncDelta delta,
            final ProvisioningReport result) {

        AnyObjectPatch anyObjectPatch = AnyObjectPatch.class.cast(anyPatch);

        Map.Entry<String, List<PropagationStatus>> updated =
                anyObjectProvisioningManager.update(anyObjectPatch, true);

        AnyObjectTO after = anyObjectDataBinder.getAnyObjectTO(updated.getKey());
        result.setName(getName(after));
        return after;
    }
}
