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
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.RoleSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class RoleSyncResultHandlerImpl extends AbstractSyncResultHandler implements RoleSyncResultHandler {

    protected Map<Long, String> roleOwnerMap = new HashMap<>();

    @Override
    public Map<Long, String> getRoleOwnerMap() {
        return this.roleOwnerMap;
    }

    @Override
    protected AttributableUtil getAttributableUtil() {
        return attrUtilFactory.getInstance(AttributableType.ROLE);
    }

    @Override
    protected String getName(final AbstractSubjectTO subjectTO) {
        return RoleTO.class.cast(subjectTO).getName();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long key) {
        try {
            return roleTransfer.getRoleTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving role {}", key, e);
            return null;
        }
    }

    @Override
    protected AbstractSubjectMod getSubjectMod(
            final AbstractSubjectTO subjectTO, final SyncDelta delta) {

        return connObjectUtil.getAttributableMod(
                subjectTO.getKey(),
                delta.getObject(),
                subjectTO,
                profile.getTask(),
                attrUtilFactory.getInstance(AttributableType.ROLE));
    }

    @Override
    protected AbstractSubjectTO create(
            final AbstractSubjectTO subjectTO, final SyncDelta _delta, final ProvisioningResult result) {

        RoleTO roleTO = RoleTO.class.cast(subjectTO);

        Map.Entry<Long, List<PropagationStatus>> created = roleProvisioningManager.create(roleTO, roleOwnerMap,
                Collections.singleton(profile.getTask().getResource().getKey()));

        roleTO = roleTransfer.getRoleTO(created.getKey());

        result.setId(created.getKey());
        result.setName(getName(subjectTO));

        return roleTO;
    }

    @Override
    protected AbstractSubjectTO link(
            final AbstractSubjectTO before,
            final ProvisioningResult result,
            final boolean unlink) {

        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(before.getKey());

        if (unlink) {
            roleMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            roleMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        return roleTransfer.getRoleTO(rwfAdapter.update(roleMod).getResult());
    }

    @Override
    protected AbstractSubjectTO update(
            final AbstractSubjectTO before,
            final AbstractSubjectMod subjectMod,
            final SyncDelta delta,
            final ProvisioningResult result) {

        RoleMod roleMod = RoleMod.class.cast(subjectMod);

        Map.Entry<Long, List<PropagationStatus>> updated = roleProvisioningManager.update(roleMod);

        //moved after role provisioning manager
        String roleOwner = null;
        for (AttrMod attrMod : roleMod.getPlainAttrsToUpdate()) {
            if (attrMod.getSchema().isEmpty()) {
                roleOwner = attrMod.getValuesToBeAdded().iterator().next();
            }
        }
        if (roleOwner != null) {
            roleOwnerMap.put(updated.getKey(), roleOwner);
        }

        final RoleTO after = roleTransfer.getRoleTO(updated.getKey());

        result.setName(getName(after));

        return after;
    }

    @Override
    protected void deprovision(final Long id, final boolean unlink) {

        taskExecutor.execute(
                propagationManager.getRoleDeleteTaskIds(id, profile.getTask().getResource().getKey()));

        if (unlink) {
            final UserMod userMod = new UserMod();
            userMod.setKey(id);
            userMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        }
    }

    @Override
    protected void delete(final Long id) {
        try {
            taskExecutor.execute(
                    propagationManager.getRoleDeleteTaskIds(id, profile.getTask().getResource().getKey()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate user " + id, e);
        }

        roleProvisioningManager.delete(id);
    }
}
