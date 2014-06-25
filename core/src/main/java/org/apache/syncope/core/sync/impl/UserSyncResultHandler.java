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
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.mod.AbstractSubjectMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class UserSyncResultHandler extends AbstractSubjectSyncResultHandler {

    @Override
    protected AttributableUtil getAttributableUtil() {
        return AttributableUtil.getInstance(AttributableType.USER);
    }

    @Override
    protected String getName(final AbstractSubjectTO subjectTO) {
        return UserTO.class.cast(subjectTO).getUsername();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long id) {
        return userDataBinder.getUserTO(id);
    }

    @Override
    protected AbstractSubjectMod getSubjectMod(
            final AbstractSubjectTO subjectTO, final SyncDelta delta) {

        return connObjectUtil.getAttributableMod(
                subjectTO.getId(),
                delta.getObject(),
                subjectTO,
                profile.getSyncTask(),
                AttributableUtil.getInstance(AttributableType.USER));
    }

    @Override
    protected AbstractSubjectTO create(
            final AbstractSubjectTO subjectTO, final SyncDelta delta, final SyncResult result) {

        UserTO userTO = UserTO.class.cast(subjectTO);

        Boolean enabled = syncUtilities.readEnabled(delta.getObject(), profile.getSyncTask());
        WorkflowResult<Map.Entry<Long, Boolean>> created =
                uwfAdapter.create(userTO, true, enabled);

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(created,
                userTO.getPassword(), userTO.getVirAttrs(),
                Collections.singleton(profile.getSyncTask().getResource().getName()),
                userTO.getMemberships());

        taskExecutor.execute(tasks);

        userTO = userDataBinder.getUserTO(created.getResult().getKey());

        result.setId(created.getResult().getKey());
        result.setName(getName(subjectTO));

        return userTO;
    }

    @Override
    protected AbstractSubjectTO update(
            final AbstractSubjectTO before,
            final AbstractSubjectMod subjectMod,
            final SyncDelta delta, final SyncResult result)
            throws Exception {

        final UserMod userMod = UserMod.class.cast(subjectMod);

        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userMod);
        } catch (Exception e) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", before.getId(), e);

            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            updated = new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                    new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
        }

        final Boolean enabled = syncUtilities.readEnabled(delta.getObject(), profile.getSyncTask());
        if (enabled != null) {
            SyncopeUser user = userDAO.find(before.getId());

            WorkflowResult<Long> enableUpdate = null;
            if (user.isSuspended() == null) {
                enableUpdate = uwfAdapter.activate(before.getId(), null);
            } else if (enabled && user.isSuspended()) {
                enableUpdate = uwfAdapter.reactivate(before.getId());
            } else if (!enabled && !user.isSuspended()) {
                enableUpdate = uwfAdapter.suspend(before.getId());
            }

            if (enableUpdate != null) {
                if (enableUpdate.getPropByRes() != null) {
                    updated.getPropByRes().merge(enableUpdate.getPropByRes());
                    updated.getPropByRes().purge();
                }
                updated.getPerformedTasks().addAll(enableUpdate.getPerformedTasks());
            }
        }

        final List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                updated, updated.getResult().getKey().getPassword() != null,
                Collections.singleton(profile.getSyncTask().getResource().getName()));

        taskExecutor.execute(tasks);

        final UserTO after = userDataBinder.getUserTO(updated.getResult().getKey().getId());

        result.setName(getName(after));
        return after;
    }

    @Override
    protected void delete(final Long id) {
        try {
            taskExecutor.execute(
                    propagationManager.getUserDeleteTaskIds(id, profile.getSyncTask().getResource().getName()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate user " + id, e);
        }

        uwfAdapter.delete(id);
    }
}
