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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.sync.AnyObjectPushResultHandler;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

public class AnyObjectPushResultHandlerImpl extends AbstractPushResultHandler implements AnyObjectPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.GROUP);
    }

    @Override
    protected Any<?, ?, ?> deprovision(final Any<?, ?, ?> sbj) {
        AnyObjectTO before = anyObjectDataBinder.getAnyObjectTO(AnyObject.class.cast(sbj));

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getAnyObjectDeleteTasks(before.getKey(), noPropResources));

        return anyObjectDAO.authFind(before.getKey());
    }

    @Override
    protected Any<?, ?, ?> provision(final Any<?, ?, ?> sbj, final Boolean enabled) {
        AnyObjectTO before = anyObjectDataBinder.getAnyObjectTO(AnyObject.class.cast(sbj));

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.getAnyObjectCreateTasks(
                before.getKey(),
                Collections.unmodifiableCollection(before.getVirAttrs()),
                propByRes,
                noPropResources));

        return anyObjectDAO.authFind(before.getKey());
    }

    @Override
    protected Any<?, ?, ?> link(final Any<?, ?, ?> sbj, final Boolean unlink) {
        AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(sbj.getKey());

        if (unlink) {
            anyObjectMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        } else {
            anyObjectMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        }

        awfAdapter.update(anyObjectMod);

        return anyObjectDAO.authFind(sbj.getKey());
    }

    @Override
    protected Any<?, ?, ?> unassign(final Any<?, ?, ?> sbj) {
        AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(sbj.getKey());
        anyObjectMod.getResourcesToRemove().add(profile.getTask().getResource().getKey());
        awfAdapter.update(anyObjectMod);
        return deprovision(sbj);
    }

    @Override
    protected Any<?, ?, ?> assign(final Any<?, ?, ?> sbj, final Boolean enabled) {
        AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(sbj.getKey());
        anyObjectMod.getResourcesToAdd().add(profile.getTask().getResource().getKey());
        awfAdapter.update(anyObjectMod);
        return provision(sbj, enabled);
    }

    @Override
    protected String getName(final Any<?, ?, ?> any) {
        return StringUtils.EMPTY;
    }

    @Override
    protected AnyTO getAnyTO(final long key) {
        try {
            return anyObjectDataBinder.getAnyObjectTO(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected Any<?, ?, ?> getAny(final long key) {
        try {
            return anyObjectDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving anyObject {}", key, e);
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
