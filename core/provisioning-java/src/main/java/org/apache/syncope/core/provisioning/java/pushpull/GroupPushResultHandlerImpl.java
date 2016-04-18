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
package org.apache.syncope.core.provisioning.java.pushpull;

import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPushResultHandler;

public class GroupPushResultHandlerImpl extends AbstractPushResultHandler implements GroupPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.GROUP);
    }

    @Override
    protected String getName(final Any<?> any) {
        return Group.class.cast(any).getName();
    }

    @Override
    protected Any<?> getAny(final String key) {
        try {
            return groupDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving group {}", key, e);
            return null;
        }
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return groupDataBinder.getGroupTO(key);
    }

    @Override
    protected AnyPatch newPatch(final String key) {
        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    protected WorkflowResult<String> update(final AnyPatch patch) {
        return gwfAdapter.update((GroupPatch) patch);
    }

}
