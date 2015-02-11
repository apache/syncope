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
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Mapping;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.sync.RolePushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class RolePushResultHandlerImpl extends AbstractPushResultHandler implements RolePushResultHandler {

    @Override
    protected Subject<?, ?, ?> deprovision(final Subject<?, ?, ?> sbj) {
        final RoleTO before = roleTransfer.getRoleTO(Role.class.cast(sbj));

        final List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getRoleDeleteTaskIds(before.getKey(), noPropResources));

        return roleDAO.authFetch(before.getKey());
    }

    @Override
    protected Subject<?, ?, ?> provision(final Subject<?, ?, ?> sbj, final Boolean enabled) {
        final RoleTO before = roleTransfer.getRoleTO(Role.class.cast(sbj));

        final List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getRoleCreateTaskIds(
                before.getKey(),
                Collections.unmodifiableCollection(before.getVirAttrs()),
                propByRes,
                noPropResources));

        return roleDAO.authFetch(before.getKey());
    }

    @Override
    protected Subject<?, ?, ?> link(final Subject<?, ?, ?> sbj, final Boolean unlink) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(sbj.getKey());

        if (unlink) {
            roleMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            roleMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        rwfAdapter.update(roleMod);

        return roleDAO.authFetch(sbj.getKey());
    }

    @Override
    protected Subject<?, ?, ?> unassign(final Subject<?, ?, ?> sbj) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(sbj.getKey());
        roleMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        rwfAdapter.update(roleMod);
        return deprovision(sbj);
    }

    @Override
    protected Subject<?, ?, ?> assign(final Subject<?, ?, ?> sbj, final Boolean enabled) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(sbj.getKey());
        roleMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        rwfAdapter.update(roleMod);
        return provision(sbj, enabled);
    }

    @Override
    protected String getName(final Subject<?, ?, ?> subject) {
        return Role.class.cast(subject).getName();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long key) {
        try {
            return roleTransfer.getRoleTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected Subject<?, ?, ?> getSubject(final long key) {
        try {
            return roleDAO.authFetch(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving role {}", key, e);
            return null;
        }
    }

    @Override
    protected ConnectorObject getRemoteObject(final String accountId) {
        ConnectorObject obj = null;

        try {
            final Uid uid = new Uid(accountId);

            obj = profile.getConnector().getObject(
                    ObjectClass.GROUP,
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
        return profile.getTask().getResource().getRmapping();
    }
}
