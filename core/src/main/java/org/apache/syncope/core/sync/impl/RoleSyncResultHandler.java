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
package org.apache.syncope.core.sync.impl;

import static org.apache.syncope.core.sync.impl.AbstractSyncopeResultHandler.LOG;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.AbstractSubjectMod;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class RoleSyncResultHandler extends AbstractSubjectSyncResultHandler {

    protected Map<Long, String> roleOwnerMap = new HashMap<Long, String>();

    public Map<Long, String> getRoleOwnerMap() {
        return this.roleOwnerMap;
    }

    @Override
    protected AttributableUtil getAttributableUtil() {
        return AttributableUtil.getInstance(AttributableType.ROLE);
    }

    @Override
    protected String getName(final AbstractSubjectTO subjectTO) {
        return RoleTO.class.cast(subjectTO).getName();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long id) {
        try {
            return roleDataBinder.getRoleTO(id);
        } catch (Exception e) {
            LOG.warn("Error retrieving role {}", id, e);
            return null;
        }
    }

    @Override
    protected AbstractSubjectMod getSubjectMod(
            final AbstractSubjectTO subjectTO, final SyncDelta delta) {

        return connObjectUtil.getAttributableMod(
                subjectTO.getId(),
                delta.getObject(),
                subjectTO,
                profile.getSyncTask(),
                AttributableUtil.getInstance(AttributableType.ROLE));
    }

    @Override
    protected AbstractSubjectTO create(
            final AbstractSubjectTO subjectTO, final SyncDelta _delta, final SyncResult result) {

        RoleTO roleTO = RoleTO.class.cast(subjectTO);

        Map.Entry<Long, List<PropagationStatus>> created = roleProvisioningManager.createInSync(roleTO, roleOwnerMap,
                Collections.singleton(profile.getSyncTask().getResource().getName()));

        roleTO = roleDataBinder.getRoleTO(created.getKey());

        result.setId(created.getKey());
        result.setName(getName(subjectTO));

        return roleTO;
    }

    @Override
    protected AbstractSubjectTO link(
            final AbstractSubjectTO before,
            final SyncResult result,
            final boolean unlink)
            throws Exception {

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(before.getId());

        if (unlink) {
            roleMod.getResourcesToRemove().add(profile.getSyncTask().getResource().getName());
        } else {
            roleMod.getResourcesToAdd().add(profile.getSyncTask().getResource().getName());
        }

        return userDataBinder.getUserTO(rwfAdapter.update(roleMod).getResult());
    }

    @Override
    protected AbstractSubjectTO update(
            final AbstractSubjectTO before,
            final AbstractSubjectMod subjectMod,
            final SyncDelta delta,
            final SyncResult result)
            throws Exception {

        RoleMod roleMod = RoleMod.class.cast(subjectMod);

        Map.Entry<Long, List<PropagationStatus>> updated = roleProvisioningManager.update(roleMod);

        //moved after role provisioning manager
        String roleOwner = null;
        for (AttributeMod attrMod : roleMod.getAttrsToUpdate()) {
            if (attrMod.getSchema().isEmpty()) {
                roleOwner = attrMod.getValuesToBeAdded().iterator().next();
            }
        }
        if (roleOwner != null) {
            roleOwnerMap.put(updated.getKey(), roleOwner);
        }

        final RoleTO after = roleDataBinder.getRoleTO(updated.getKey());

        result.setName(getName(after));

        return after;
    }

    @Override
    protected void deprovision(final Long id, final boolean unlink) {

        taskExecutor.execute(
                propagationManager.getRoleDeleteTaskIds(id, profile.getSyncTask().getResource().getName()));

        if (unlink) {
            final UserMod userMod = new UserMod();
            userMod.setId(id);
            userMod.getResourcesToRemove().add(profile.getSyncTask().getResource().getName());
        }
    }

    @Override
    protected void delete(final Long id) {
        try {
            taskExecutor.execute(
                    propagationManager.getRoleDeleteTaskIds(id, profile.getSyncTask().getResource().getName()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate user " + id, e);
        }

        roleProvisioningManager.delete(id);
    }
}
