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
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.sync.UserPushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class UserPushResultHandlerImpl extends AbstractPushResultHandler implements UserPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Override
    protected Any<?, ?, ?> deprovision(final Any<?, ?, ?> sbj) {
        UserTO before = userDataBinder.getUserTO(sbj.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUserDeleteTasks(before.getKey(),
                Collections.singleton(profile.getTask().getResource().getKey()), noPropResources));

        return userDAO.authFind(before.getKey());
    }

    @Override
    protected Any<?, ?, ?> provision(final Any<?, ?, ?> sbj, final Boolean enabled) {
        UserTO before = userDataBinder.getUserTO(sbj.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getUserCreateTasks(
                before.getKey(),
                enabled,
                propByRes,
                null,
                Collections.unmodifiableCollection(before.getVirAttrs()),
                noPropResources));

        return userDAO.authFind(before.getKey());
    }

    @Override
    protected Any<?, ?, ?> link(final Any<?, ?, ?> sbj, final Boolean unlink) {
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(sbj.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());

        uwfAdapter.update(userPatch);

        return userDAO.authFind(userPatch.getKey());
    }

    @Override
    protected Any<?, ?, ?> unassign(final Any<?, ?, ?> sbj) {
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(sbj.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.DELETE).
                value(profile.getTask().getResource().getKey()).build());

        uwfAdapter.update(userPatch);

        return deprovision(sbj);
    }

    @Override
    protected Any<?, ?, ?> assign(final Any<?, ?, ?> sbj, final Boolean enabled) {
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(sbj.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).
                value(profile.getTask().getResource().getKey()).build());
        uwfAdapter.update(userPatch);

        return provision(sbj, enabled);
    }

    @Override
    protected String getName(final Any<?, ?, ?> any) {
        return User.class.cast(any).getUsername();
    }

    @Override
    protected AnyTO getAnyTO(final long key) {
        try {
            return userDataBinder.getUserTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected Any<?, ?, ?> getAny(final long key) {
        try {
            return userDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
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
