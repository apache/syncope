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
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
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
    protected String getName(final AnyCR anyCR) {
        return GroupCR.class.cast(anyCR).getName();
    }

    @Override
    protected ProvisioningManager<?, ?, ?> getProvisioningManager() {
        return groupProvisioningManager;
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return groupDataBinder.getGroupTO(key);
    }

    @Override
    protected WorkflowResult<? extends AnyUR> update(final AnyUR req) {
        return gwfAdapter.update((GroupUR) req);
    }

    @Override
    protected AnyTO doCreate(final AnyCR anyCR, final SyncDelta delta) {
        GroupCR groupCR = GroupCR.class.cast(anyCR);

        Map.Entry<String, List<PropagationStatus>> created = groupProvisioningManager.create(
                groupCR,
                groupOwnerMap,
                Collections.singleton(profile.getTask().getResource().getKey()),
                true);

        return getAnyTO(created.getKey());
    }

    @Override
    protected AnyUR doUpdate(
            final AnyTO before,
            final AnyUR req,
            final SyncDelta delta,
            final ProvisioningReport result) {

        GroupUR groupUR = GroupUR.class.cast(req);

        Pair<GroupUR, List<PropagationStatus>> updated = groupProvisioningManager.update(
                groupUR, Collections.singleton(profile.getTask().getResource().getKey()), true);

        String groupOwner = null;
        for (AttrPatch attrPatch : groupUR.getPlainAttrs()) {
            if (attrPatch.getOperation() == PatchOperation.ADD_REPLACE && attrPatch.getAttr() != null
                    && attrPatch.getAttr().getSchema().isEmpty() && !attrPatch.getAttr().getValues().isEmpty()) {

                groupOwner = attrPatch.getAttr().getValues().get(0);
            }
        }
        if (groupOwner != null) {
            groupOwnerMap.put(updated.getLeft().getKey(), groupOwner);
        }

        return req;
    }

}
