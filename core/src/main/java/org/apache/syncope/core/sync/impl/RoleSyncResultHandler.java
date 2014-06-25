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
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.AttributeTO;
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
        return roleDataBinder.getRoleTO(id);
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

        WorkflowResult<Long> created = rwfAdapter.create(roleTO);
        AttributeTO roleOwner = roleTO.getAttrMap().get(StringUtils.EMPTY);
        if (roleOwner != null) {
            roleOwnerMap.put(created.getResult(), roleOwner.getValues().iterator().next());
        }

        EntitlementUtil.extendAuthContext(created.getResult());

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created,
                roleTO.getVirAttrs(), Collections.singleton(profile.getSyncTask().getResource().getName()));

        taskExecutor.execute(tasks);

        roleTO = roleDataBinder.getRoleTO(created.getResult());

        result.setId(created.getResult());
        result.setName(getName(subjectTO));

        return roleTO;
    }

    @Override
    protected AbstractSubjectTO update(
            final AbstractSubjectTO before,
            final AbstractSubjectMod subjectMod,
            final SyncDelta delta,
            final SyncResult result)
            throws Exception {

        RoleMod roleMod = RoleMod.class.cast(subjectMod);

        final WorkflowResult<Long> updated = rwfAdapter.update(roleMod);
        String roleOwner = null;
        for (AttributeMod attrMod : roleMod.getAttrsToUpdate()) {
            if (attrMod.getSchema().isEmpty()) {
                roleOwner = attrMod.getValuesToBeAdded().iterator().next();
            }
        }
        if (roleOwner != null) {
            roleOwnerMap.put(updated.getResult(), roleOwner);
        }

        List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                roleMod.getVirAttrsToRemove(),
                roleMod.getVirAttrsToUpdate(),
                Collections.singleton(profile.getSyncTask().getResource().getName()));

        taskExecutor.execute(tasks);

        final RoleTO after = roleDataBinder.getRoleTO(updated.getResult());
        result.setName(getName(after));

        return after;
    }

    @Override
    protected void delete(Long id) {
        try {
            taskExecutor.execute(
                    propagationManager.getRoleDeleteTaskIds(id, profile.getSyncTask().getResource().getName()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate user " + id, e);
        }

        rwfAdapter.delete(id);
    }
}
