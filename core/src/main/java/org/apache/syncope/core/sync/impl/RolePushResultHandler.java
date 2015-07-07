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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.AbstractMapping;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.util.AttributableUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class RolePushResultHandler extends AbstractSubjectPushResultHandler {

    @Override
    protected AttributableUtil getAttributableUtil() {
        return AttributableUtil.getInstance(AttributableType.ROLE);
    }

    @Override
    protected AbstractSubject deprovision(final AbstractSubject sbj) {
        final RoleTO before = roleDataBinder.getRoleTO(SyncopeRole.class.cast(sbj), true);

        final List<String> noPropResources = new ArrayList<String>(before.getResources());
        noPropResources.remove(profile.getSyncTask().getResource().getName());

        taskExecutor.execute(propagationManager.getRoleDeleteTaskIds(before.getId(), noPropResources));

        return roleDataBinder.getRoleFromId(before.getId());
    }

    @Override
    protected AbstractSubject provision(final AbstractSubject sbj, final Boolean enabled) {
        final RoleTO before = roleDataBinder.getRoleTO(SyncopeRole.class.cast(sbj), true);

        final List<String> noPropResources = new ArrayList<String>(before.getResources());
        noPropResources.remove(profile.getSyncTask().getResource().getName());

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getSyncTask().getResource().getName());

        taskExecutor.execute(propagationManager.getRoleCreateTaskIds(
                before.getId(),
                Collections.unmodifiableCollection(before.getVirAttrs()),
                propByRes,
                noPropResources));

        return roleDataBinder.getRoleFromId(before.getId());
    }

    @Override
    protected AbstractSubject link(final AbstractSubject sbj, final Boolean unlink) {

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(sbj.getId());

        if (unlink) {
            roleMod.getResourcesToRemove().add(profile.getSyncTask().getResource().getName());
        } else {
            roleMod.getResourcesToAdd().add(profile.getSyncTask().getResource().getName());
        }

        rwfAdapter.update(roleMod);

        return roleDataBinder.getRoleFromId(sbj.getId());
    }

    @Override
    protected AbstractSubject unassign(final AbstractSubject sbj) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setId(sbj.getId());
        roleMod.getResourcesToRemove().add(profile.getSyncTask().getResource().getName());
        rwfAdapter.update(roleMod);
        return deprovision(sbj);
    }

    @Override
    protected AbstractSubject assign(final AbstractSubject sbj, final Boolean enabled) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setId(sbj.getId());
        roleMod.getResourcesToAdd().add(profile.getSyncTask().getResource().getName());
        rwfAdapter.update(roleMod);
        return provision(sbj, enabled);
    }

    @Override
    protected String getName(final AbstractSubject subject) {
        return SyncopeRole.class.cast(subject).getName();
    }

    @Override
    protected AbstractSubjectTO getSubjectTO(final long id) {
        try {
            return roleDataBinder.getRoleTO(id);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", id, e);
            return null;
        }
    }

    @Override
    protected AbstractSubject getSubject(final long id) {
        try {
            return roleDataBinder.getRoleFromId(id);
        } catch (Exception e) {
            LOG.warn("Error retrieving role {}", id, e);
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
                    profile.getConnector().getOperationOptions(Collections.<AbstractMappingItem>emptySet()));

        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }
        return obj;
    }

    @Override
    protected AbstractMapping getMapping() {
        return profile.getSyncTask().getResource().getRmapping();
    }
}
