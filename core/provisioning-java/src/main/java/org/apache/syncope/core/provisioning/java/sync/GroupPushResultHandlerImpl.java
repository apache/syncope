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
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.sync.GroupPushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class GroupPushResultHandlerImpl extends AbstractPushResultHandler implements GroupPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.GROUP);
    }

    @Override
    protected Any<?, ?, ?> deprovision(final Any<?, ?, ?> sbj) {
        GroupTO before = groupDataBinder.getGroupTO(Group.class.cast(sbj), true);

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getGroupDeleteTasks(before.getKey(), noPropResources));

        return groupDAO.authFind(before.getKey());
    }

    @Override
    protected Any<?, ?, ?> provision(final Any<?, ?, ?> sbj, final Boolean enabled) {
        GroupTO before = groupDataBinder.getGroupTO(Group.class.cast(sbj), true);

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getGroupCreateTasks(
                before.getKey(),
                Collections.unmodifiableCollection(before.getVirAttrs()),
                propByRes,
                noPropResources));

        return groupDAO.authFind(before.getKey());
    }

    @Override
    protected Any<?, ?, ?> link(final Any<?, ?, ?> sbj, final Boolean unlink) {
        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(sbj.getKey());

        groupPatch.getResources().add(new StringPatchItem.Builder().
                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        gwfAdapter.update(groupPatch);

        return groupDAO.authFind(sbj.getKey());
    }

    @Override
    protected Any<?, ?, ?> unassign(final Any<?, ?, ?> sbj) {
        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(sbj.getKey());
        groupPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.DELETE).
                value(profile.getTask().getResource().getKey()).build());

        gwfAdapter.update(groupPatch);

        return deprovision(sbj);
    }

    @Override
    protected Any<?, ?, ?> assign(final Any<?, ?, ?> sbj, final Boolean enabled) {
        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(sbj.getKey());
        groupPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        gwfAdapter.update(groupPatch);

        return provision(sbj, enabled);
    }

    @Override
    protected String getName(final Any<?, ?, ?> any) {
        return Group.class.cast(any).getName();
    }

    @Override
    protected AnyTO getAnyTO(final long key) {
        try {
            return groupDataBinder.getGroupTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected Any<?, ?, ?> getAny(final long key) {
        try {
            return groupDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving group {}", key, e);
            return null;
        }
    }

    @Override
    protected ConnectorObject getRemoteObject(final String connObjectKey, final ObjectClass objectClass) {
        ConnectorObject obj = null;
        try {
            Uid uid = new Uid(connObjectKey);

            obj = profile.getConnector().getObject(
                    objectClass,
                    uid,
                    profile.getConnector().getOperationOptions(Collections.<MappingItem>emptySet()));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", connObjectKey, ignore);
        }

        return obj;
    }
}
