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
import org.apache.syncope.common.lib.mod.AbstractSubjectMod;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.GroupSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class GroupSyncResultHandlerImpl extends AbstractSyncResultHandler implements GroupSyncResultHandler {

    protected Map<Long, String> groupOwnerMap = new HashMap<>();

    @Override
    public Map<Long, String> getGroupOwnerMap() {
        return this.groupOwnerMap;
    }

    @Override
    protected AttributableUtils getAttributableUtils() {
        return attrUtilsFactory.getInstance(AttributableType.GROUP);
    }

    @Override
    protected String getName(final AbstractSubjectTO subjectTO) {
        return GroupTO.class.cast(subjectTO).getName();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long key) {
        try {
            return groupDataBinder.getGroupTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving group {}", key, e);
            return null;
        }
    }

    @Override
    protected AbstractSubjectMod getSubjectMod(final AbstractSubjectTO subjectTO, final SyncDelta delta) {
        return connObjectUtils.getAttributableMod(
                subjectTO.getKey(),
                delta.getObject(),
                subjectTO,
                profile.getTask(),
                attrUtilsFactory.getInstance(AttributableType.GROUP));
    }

    @Override
    protected AbstractSubjectTO doCreate(
            final AbstractSubjectTO subjectTO, final SyncDelta delta, final ProvisioningResult result) {

        GroupTO groupTO = GroupTO.class.cast(subjectTO);

        Map.Entry<Long, List<PropagationStatus>> created = groupProvisioningManager.create(groupTO, groupOwnerMap,
                Collections.singleton(profile.getTask().getResource().getKey()));

        groupTO = groupDataBinder.getGroupTO(created.getKey());

        result.setKey(created.getKey());
        result.setName(getName(subjectTO));

        return groupTO;
    }

    @Override
    protected AbstractSubjectTO doLink(
            final AbstractSubjectTO before,
            final ProvisioningResult result,
            final boolean unlink) {

        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(before.getKey());

        if (unlink) {
            groupMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            groupMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        return groupDataBinder.getGroupTO(gwfAdapter.update(groupMod).getResult());
    }

    @Override
    protected AbstractSubjectTO doUpdate(
            final AbstractSubjectTO before,
            final AbstractSubjectMod subjectMod,
            final SyncDelta delta,
            final ProvisioningResult result) {

        GroupMod groupMod = GroupMod.class.cast(subjectMod);

        Map.Entry<Long, List<PropagationStatus>> updated = groupProvisioningManager.update(groupMod);

        // moved after group provisioning manager
        String groupOwner = null;
        for (AttrMod attrMod : groupMod.getPlainAttrsToUpdate()) {
            if (attrMod.getSchema().isEmpty()) {
                groupOwner = attrMod.getValuesToBeAdded().iterator().next();
            }
        }
        if (groupOwner != null) {
            groupOwnerMap.put(updated.getKey(), groupOwner);
        }

        final GroupTO after = groupDataBinder.getGroupTO(updated.getKey());

        result.setName(getName(after));

        return after;
    }

    @Override
    protected void doDeprovision(final Long id, final boolean unlink) {
        taskExecutor.execute(
                propagationManager.getGroupDeleteTasks(id, profile.getTask().getResource().getKey()));

        if (unlink) {
            final UserMod userMod = new UserMod();
            userMod.setKey(id);
            userMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        }
    }

    @Override
    protected void doDelete(final Long id) {
        try {
            taskExecutor.execute(
                    propagationManager.getGroupDeleteTasks(id, profile.getTask().getResource().getKey()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate user " + id, e);
        }

        groupProvisioningManager.delete(id);
    }
}
