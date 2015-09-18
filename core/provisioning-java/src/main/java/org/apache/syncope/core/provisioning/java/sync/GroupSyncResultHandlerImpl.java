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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.GroupSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class GroupSyncResultHandlerImpl extends AbstractSyncResultHandler implements GroupSyncResultHandler {

    protected final Map<Long, String> groupOwnerMap = new HashMap<>();

    @Override
    public Map<Long, String> getGroupOwnerMap() {
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
    protected AnyTO getAnyTO(final long key) {
        try {
            return groupDataBinder.getGroupTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving group {}", key, e);
            return null;
        }
    }

    @Override
    protected AnyTO doCreate(final AnyTO anyTO, final SyncDelta delta, final ProvisioningResult result) {
        GroupTO groupTO = GroupTO.class.cast(anyTO);

        Map.Entry<Long, List<PropagationStatus>> created = groupProvisioningManager.create(groupTO, groupOwnerMap,
                Collections.singleton(profile.getTask().getResource().getKey()));

        result.setKey(created.getKey());

        return groupDataBinder.getGroupTO(created.getKey());
    }

    @Override
    protected AnyTO doLink(
            final AnyTO before,
            final ProvisioningResult result,
            final boolean unlink) {

        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(before.getKey());
        groupPatch.getResources().add(new StringPatchItem.Builder().
                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        return groupDataBinder.getGroupTO(gwfAdapter.update(groupPatch).getResult());
    }

    @Override
    protected AnyTO doUpdate(
            final AnyTO before,
            final AnyPatch anyPatch,
            final SyncDelta delta,
            final ProvisioningResult result) {

        GroupPatch groupPatch = GroupPatch.class.cast(anyPatch);

        Map.Entry<Long, List<PropagationStatus>> updated = groupProvisioningManager.update(groupPatch);

        // moved after group provisioning manager
        String groupOwner = null;
        for (AttrPatch attrPatch : groupPatch.getPlainAttrs()) {
            if (attrPatch.getOperation() == PatchOperation.ADD_REPLACE && attrPatch.getAttrTO() != null
                    && attrPatch.getAttrTO().getSchema().isEmpty() && !attrPatch.getAttrTO().getValues().isEmpty()) {

                groupOwner = attrPatch.getAttrTO().getValues().get(0);
            }
        }
        if (groupOwner != null) {
            groupOwnerMap.put(updated.getKey(), groupOwner);
        }

        GroupTO after = groupDataBinder.getGroupTO(updated.getKey());

        result.setName(getName(after));

        return after;
    }

    @Override
    protected void doDeprovision(final Long key, final boolean unlink) {
        taskExecutor.execute(propagationManager.getGroupDeleteTasks(key, profile.getTask().getResource().getKey()));

        if (unlink) {
            GroupPatch groupPatch = new GroupPatch();
            groupPatch.setKey(key);
            groupPatch.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.DELETE).
                    value(profile.getTask().getResource().getKey()).build());
        }
    }

    @Override
    protected void doDelete(final Long key) {
        try {
            taskExecutor.execute(propagationManager.getGroupDeleteTasks(key, profile.getTask().getResource().getKey()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate group " + key, e);
        }

        groupProvisioningManager.delete(key);
    }
}
