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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.Mapping;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.sync.UserPushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class UserPushResultHandlerImpl extends AbstractPushResultHandler implements UserPushResultHandler {

    @Override
    protected AttributableUtil getAttributableUtil() {
        return attrUtilFactory.getInstance(AttributableType.USER);
    }

    @Override
    protected Subject<?, ?, ?> deprovision(final Subject<?, ?, ?> sbj) {
        final UserTO before = userTransfer.getUserTO(sbj.getKey());

        final List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUserDeleteTaskIds(before.getKey(),
                Collections.singleton(profile.getTask().getResource().getKey()), noPropResources));

        return userDAO.authFetch(before.getKey());
    }

    @Override
    protected Subject<?, ?, ?> provision(final Subject<?, ?, ?> sbj, final Boolean enabled) {
        final UserTO before = userTransfer.getUserTO(sbj.getKey());

        final List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUserCreateTaskIds(
                before.getKey(),
                enabled,
                propByRes,
                null,
                Collections.unmodifiableCollection(before.getVirAttrs()),
                Collections.unmodifiableCollection(before.getMemberships()),
                noPropResources));

        return userDAO.authFetch(before.getKey());
    }

    @Override
    protected Subject<?, ?, ?> link(final Subject<?, ?, ?> sbj, final Boolean unlink) {
        final UserMod userMod = new UserMod();
        userMod.setKey(sbj.getKey());

        if (unlink) {
            userMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            userMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        uwfAdapter.update(userMod);

        return userDAO.authFetch(userMod.getKey());
    }

    @Override
    protected Subject<?, ?, ?> unassign(final Subject<?, ?, ?> sbj) {
        final UserMod userMod = new UserMod();
        userMod.setKey(sbj.getKey());
        userMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        uwfAdapter.update(userMod);
        return deprovision(sbj);
    }

    @Override
    protected Subject<?, ?, ?> assign(final Subject<?, ?, ?> sbj, final Boolean enabled) {
        final UserMod userMod = new UserMod();
        userMod.setKey(sbj.getKey());
        userMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        uwfAdapter.update(userMod);
        return provision(sbj, enabled);
    }

    @Override
    protected String getName(final Subject<?, ?, ?> subject) {
        return User.class.cast(subject).getUsername();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long key) {
        try {
            return userTransfer.getUserTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected Subject<?, ?, ?> getSubject(final long key) {
        try {
            return userDAO.authFetch(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected ConnectorObject getRemoteObject(final String accountId) {
        ConnectorObject obj = null;

        try {
            final Uid uid = new Uid(accountId);

            obj = profile.getConnector().getObject(
                    ObjectClass.ACCOUNT,
                    uid,
                    profile.getConnector().getOperationOptions(Collections.<MappingItem>emptySet()));

        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }
        return obj;
    }

    @Override
    protected Mapping<?> getMapping() {
        return profile.getTask().getResource().getUmapping();
    }
}
