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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.core.provisioning.api.pushpull.LiveSyncDeltaMapper;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.LiveSyncDelta;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;

public class SyncReplLiveSyncDeltaMapper implements LiveSyncDeltaMapper {

    protected static final Set<String> FILTERING_ATTRS = Set.of(
            Uid.NAME, Name.NAME, SyncReplInboundActions.SYNCREPL_COOKIE_NAME);

    protected SyncDelta doMap(final LiveSyncDelta liveSyncDelta) {
        String cookie = Optional.ofNullable(liveSyncDelta.getObject().
                getAttributeByName(SyncReplInboundActions.SYNCREPL_COOKIE_NAME)).
                map(AttributeUtil::getStringValue).filter(Objects::nonNull).
                orElseThrow(() -> new IllegalArgumentException(
                "Could not find the " + SyncReplInboundActions.SYNCREPL_COOKIE_NAME + " attribute"));

        SyncDeltaType syncDeltaType = liveSyncDelta.getUid().getUidValue().
                equals(liveSyncDelta.getObject().getName().getNameValue())
                ? SyncDeltaType.DELETE
                : SyncDeltaType.CREATE_OR_UPDATE;

        ConnectorObjectBuilder connObjectBuilder = new ConnectorObjectBuilder().
                setObjectClass(liveSyncDelta.getObjectClass()).
                setUid(liveSyncDelta.getUid()).
                setName(liveSyncDelta.getObject().getName());

        liveSyncDelta.getObject().getAttributes().stream().
                filter(attr -> !FILTERING_ATTRS.contains(attr.getName())).
                forEach(connObjectBuilder::addAttribute);

        return new SyncDeltaBuilder().
                setToken(new SyncToken(cookie)).
                setDeltaType(syncDeltaType).
                setObject(connObjectBuilder.build()).
                build();
    }

    @Override
    public SyncDelta map(final LiveSyncDelta liveSyncDelta, final OrgUnit orgUnit) {
        if (!orgUnit.getObjectClass().equals(liveSyncDelta.getObjectClass().getObjectClassValue())) {
            throw new IllegalArgumentException("Expected " + orgUnit.getObjectClass()
                    + ", got " + liveSyncDelta.getObjectClass().getObjectClassValue());
        }

        return doMap(liveSyncDelta);
    }

    @Override
    public SyncDelta map(final LiveSyncDelta liveSyncDelta, final Provision provision) {
        if (!provision.getObjectClass().equals(liveSyncDelta.getObjectClass().getObjectClassValue())) {
            throw new IllegalArgumentException("Expected " + provision.getObjectClass()
                    + ", got " + liveSyncDelta.getObjectClass().getObjectClassValue());
        }

        return doMap(liveSyncDelta);
    }
}
