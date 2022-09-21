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
package org.apache.syncope.core.provisioning.java.pushpull.stream;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.Connector;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveSyncConnector implements Connector {

    public static final Logger LOG = LoggerFactory.getLogger(LiveSyncConnector.class);

    private final SyncDelta syncDelta;

    public LiveSyncConnector(final SyncDelta syncDelta) {
        this.syncDelta = syncDelta;
    }

    @Override
    public Uid authenticate(final String username, final String password, final OperationOptions options) {
        return null;
    }

    @Override
    public ConnInstance getConnInstance() {
        return null;
    }

    @Override
    public Uid create(
            final ObjectClass objectClass,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final AtomicReference<Boolean> propagationAttempted) {
        return null;
    }

    @Override
    public Uid update(final ObjectClass objectClass,
                      final Uid uid,
                      final Set<Attribute> attrs,
                      final OperationOptions options,
                      final AtomicReference<Boolean> propagationAttempted) {
        return null;
    }

    @Override
    public Set<AttributeDelta> updateDelta(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<AttributeDelta> modifications,
            final OperationOptions options,
            final AtomicReference<Boolean> propagationAttempted) {
        return Set.of();
    }

    @Override
    public void delete(final ObjectClass objectClass,
                       final Uid uid,
                       final OperationOptions options,
                       final AtomicReference<Boolean> propagationAttempted) {

        // nothing to do
    }

    @Override
    public void sync(
            final ObjectClass objectClass,
            final SyncToken token,
            final SyncResultsHandler handler,
            final OperationOptions options) {
        handler.handle(this.syncDelta);
    }

    @Override
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectorObject getObject(
            final ObjectClass objectClass,
            final Attribute connObjectKey,
            final boolean ignoreCaseMatch,
            final OperationOptions options) {
        return null;
    }

    @Override
    public SearchResult search(final ObjectClass objectClass,
                               final Filter filter,
                               final SearchResultsHandler handler,
                               final OperationOptions options) {
        return null;
    }

    @Override
    public Set<ObjectClassInfo> getObjectClassInfo() {
        return Set.of();
    }

    @Override
    public void validate() {
        //nothing to do
    }

    @Override
    public void test() {
        //nothing to do
    }

    @Override
    public void dispose() {
        //nothing to do
    }
    
}
