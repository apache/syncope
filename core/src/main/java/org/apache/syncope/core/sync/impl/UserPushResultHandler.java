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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.propagation.impl.AbstractPropagationTaskExecutor;
import org.apache.syncope.core.sync.SyncResult;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class UserPushResultHandler extends AbstractSubjectPushResultHandler {

    @Override
    protected AbstractSubject deprovision(final AbstractSubject sbj, final SyncResult result) {
        final UserTO before = userDataBinder.getUserTO(sbj.getId());

        final List<String> noPropResources = new ArrayList<String>(before.getResources());
        noPropResources.remove(profile.getSyncTask().getResource().getName());

        taskExecutor.execute(propagationManager.getUserDeleteTaskIds(before.getId(), noPropResources));

        result.setId(before.getId());
        return userDataBinder.getUserFromId(before.getId());
    }

    @Override
    protected AbstractSubject provision(final AbstractSubject sbj, final Boolean enabled, final SyncResult result) {
        final UserTO before = userDataBinder.getUserTO(sbj.getId());

        final List<String> noPropResources = new ArrayList<String>(before.getResources());
        noPropResources.remove(profile.getSyncTask().getResource().getName());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getSyncTask().getResource().getName());

        taskExecutor.execute(propagationManager.getUserCreateTaskIds(
                before.getId(),
                enabled,
                propByRes,
                null,
                Collections.unmodifiableCollection(before.getVirAttrs()),
                Collections.unmodifiableCollection(before.getMemberships()),
                noPropResources));

        result.setId(before.getId());
        return userDataBinder.getUserFromId(before.getId());
    }

    @Override
    protected AbstractSubject link(
            final AbstractSubject sbj, final Boolean unlink, final SyncResult result) {

        final UserMod userMod = new UserMod();
        userMod.setId(sbj.getId());

        if (unlink) {
            userMod.getResourcesToRemove().add(profile.getSyncTask().getResource().getName());
        } else {
            userMod.getResourcesToAdd().add(profile.getSyncTask().getResource().getName());
        }

        uwfAdapter.update(userMod);

        result.setId(sbj.getId());
        return userDataBinder.getUserFromId(sbj.getId());
    }

    @Override
    protected AbstractSubject unassign(final AbstractSubject sbj, final SyncResult result) {
        final UserMod userMod = new UserMod();
        userMod.setId(sbj.getId());
        userMod.getResourcesToRemove().add(profile.getSyncTask().getResource().getName());
        uwfAdapter.update(userMod);
        return deprovision(sbj, result);
    }

    @Override
    protected AbstractSubject assign(final AbstractSubject sbj, final Boolean enabled, final SyncResult result) {
        final UserMod userMod = new UserMod();
        userMod.setId(sbj.getId());
        userMod.getResourcesToAdd().add(profile.getSyncTask().getResource().getName());
        uwfAdapter.update(userMod);
        return provision(sbj, enabled, result);
    }

    @Override
    protected AbstractSubject update(
            final AbstractSubject sbj,
            final String accountId,
            final Set<Attribute> attributes,
            final ConnectorObject beforeObj,
            final SyncResult result) {

        AbstractPropagationTaskExecutor.createOrUpdate(
                ObjectClass.ACCOUNT,
                accountId,
                attributes,
                profile.getSyncTask().getResource().getName(),
                profile.getSyncTask().getResource().getPropagationMode(),
                beforeObj,
                profile.getConnector(),
                new HashSet<String>(),
                connObjectUtil);

        result.setId(sbj.getId());
        return userDataBinder.getUserFromId(sbj.getId());
    }

    @Override
    protected String getName(final AbstractSubject subject) {
        return SyncopeUser.class.cast(subject).getUsername();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long id) {
        try {
            return userDataBinder.getUserTO(id);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", id, e);
            return null;
        }
    }

    @Override
    protected AbstractSubject getSubject(final long id) {
        try {
            return userDataBinder.getUserFromId(id);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", id, e);
            return null;
        }
    }

    @Override
    protected ConnectorObject getRemoteObject(final String accountId) {
        ConnectorObject obj = null;

        try {

            final Uid uid = new Uid(accountId);

            profile.getConnector().getObject(
                    ObjectClass.ACCOUNT,
                    uid,
                    profile.getConnector().getOperationOptions(Collections.<AbstractMappingItem>emptySet()));

        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }
        return obj;
    }
}
