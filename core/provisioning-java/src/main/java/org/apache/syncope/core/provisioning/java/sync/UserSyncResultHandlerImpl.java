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
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.mod.AbstractSubjectMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.api.sync.UserSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;

public class UserSyncResultHandlerImpl extends AbstractSyncResultHandler implements UserSyncResultHandler {

    @Override
    protected AttributableUtils getAttributableUtils() {
        return attrUtilsFactory.getInstance(AttributableType.USER);
    }

    @Override
    protected String getName(final AbstractSubjectTO subjectTO) {
        return UserTO.class.cast(subjectTO).getUsername();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long key) {
        try {
            return userDataBinder.getUserTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected AbstractSubjectMod getSubjectMod(
            final AbstractSubjectTO subjectTO, final SyncDelta delta) {

        return connObjectUtils.getAttributableMod(subjectTO.getKey(),
                delta.getObject(),
                subjectTO,
                profile.getTask(),
                getAttributableUtils());
    }

    @Override
    protected AbstractSubjectTO create(
            final AbstractSubjectTO subjectTO, final SyncDelta delta, final ProvisioningResult result) {

        UserTO userTO = UserTO.class.cast(subjectTO);

        Boolean enabled = syncUtilities.readEnabled(delta.getObject(), profile.getTask());
        Map.Entry<Long, List<PropagationStatus>> created = userProvisioningManager.create(userTO, true, true, enabled,
                Collections.singleton(profile.getTask().getResource().getKey()));

        result.setId(created.getKey());

        return userDataBinder.getUserTO(created.getKey());
    }

    @Override
    protected AbstractSubjectTO link(
            final AbstractSubjectTO before,
            final ProvisioningResult result,
            final boolean unlink) {

        final UserMod userMod = new UserMod();
        userMod.setKey(before.getKey());

        if (unlink) {
            userMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            userMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        return userDataBinder.getUserTO(uwfAdapter.update(userMod).getResult().getKey().getKey());
    }

    @Override
    protected AbstractSubjectTO update(
            final AbstractSubjectTO before,
            final AbstractSubjectMod subjectMod,
            final SyncDelta delta,
            final ProvisioningResult result) {

        final UserMod userMod = UserMod.class.cast(subjectMod);
        final Boolean enabled = syncUtilities.readEnabled(delta.getObject(), profile.getTask());

        Map.Entry<Long, List<PropagationStatus>> updated = userProvisioningManager.update(userMod, before.getKey(),
                result, enabled, Collections.singleton(profile.getTask().getResource().getKey()));

        return userDataBinder.getUserTO(updated.getKey());
    }

    @Override
    protected void deprovision(
            final Long key,
            final boolean unlink) {

        taskExecutor.execute(
                propagationManager.getUserDeleteTasks(
                        key, Collections.singleton(profile.getTask().getResource().getKey())));

        if (unlink) {
            final UserMod userMod = new UserMod();
            userMod.setKey(key);
            userMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        }
    }

    @Override
    protected void delete(final Long key) {
        try {
            userProvisioningManager.
                    delete(key, Collections.<String>singleton(profile.getTask().getResource().getKey()));
        } catch (Exception e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate user " + key, e);
        }

        uwfAdapter.delete(key);
    }
}
