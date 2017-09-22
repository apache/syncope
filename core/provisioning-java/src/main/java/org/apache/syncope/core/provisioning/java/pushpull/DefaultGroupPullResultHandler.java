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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultGroupPullResultHandler extends AbstractPullResultHandler implements GroupPullResultHandler {

    @Autowired
    private GroupProvisioningManager groupProvisioningManager;

    private final Map<String, String> groupOwnerMap = new HashMap<>();

    @Override
    public Map<String, String> getGroupOwnerMap() {
        return this.groupOwnerMap;
    }

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.GROUP);
    }

    @Override
    protected String getName(final AnyTO anyTO) {
        return GroupTO.class.cast(anyTO).getName();
    }

    @Override
    protected ProvisioningManager<?, ?> getProvisioningManager() {
        return groupProvisioningManager;
    }

    @Override
    protected Any<?> getAny(final String key) {
        try {
            return groupDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving group {}", key, e);
            return null;
        }
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return groupDataBinder.getGroupTO(key);
    }

    @Override
    protected AnyPatch newPatch(final String key) {
        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    protected WorkflowResult<? extends AnyPatch> update(final AnyPatch patch) {
        return gwfAdapter.update((GroupPatch) patch);
    }

    @Override
    protected AnyTO doCreate(final AnyTO anyTO, final SyncDelta delta) {
        GroupTO groupTO = GroupTO.class.cast(anyTO);

        Map.Entry<String, List<PropagationStatus>> created = groupProvisioningManager.create(
                groupTO,
                groupOwnerMap,
                Collections.singleton(profile.getTask().getResource().getKey()),
                true);

        return getAnyTO(created.getKey());
    }

    @Override
    protected AnyPatch doUpdate(
            final AnyTO before,
            final AnyPatch anyPatch,
            final SyncDelta delta,
            final ProvisioningReport result) {

        GroupPatch groupPatch = GroupPatch.class.cast(anyPatch);

        Pair<GroupPatch, List<PropagationStatus>> updated = groupProvisioningManager.update(
                groupPatch, Collections.singleton(profile.getTask().getResource().getKey()), true);

        String groupOwner = null;
        for (AttrPatch attrPatch : groupPatch.getPlainAttrs()) {
            if (attrPatch.getOperation() == PatchOperation.ADD_REPLACE && attrPatch.getAttrTO() != null
                    && attrPatch.getAttrTO().getSchema().isEmpty() && !attrPatch.getAttrTO().getValues().isEmpty()) {

                groupOwner = attrPatch.getAttrTO().getValues().get(0);
            }
        }
        if (groupOwner != null) {
            groupOwnerMap.put(updated.getLeft().getKey(), groupOwner);
        }

        return anyPatch;
    }

}
