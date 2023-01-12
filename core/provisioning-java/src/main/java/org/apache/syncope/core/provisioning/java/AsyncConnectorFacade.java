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
package org.apache.syncope.core.provisioning.java;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/**
 * Intercept calls to ConnectorFacade's methods and check if the corresponding connector instance has been configured to
 * allow every single operation: if not, simply do nothing.
 */
public class AsyncConnectorFacade {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncConnectorFacade.class);

    @Async
    public Future<Uid> authenticate(
            final ConnectorFacade connector,
            final String username,
            final GuardedString password,
            final OperationOptions options) {

        return CompletableFuture.completedFuture(
                connector.authenticate(ObjectClass.ACCOUNT, username, password, options));
    }

    @Async
    public Future<Uid> create(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        return CompletableFuture.completedFuture(connector.create(objectClass, attrs, options));
    }

    @Async
    public Future<Uid> update(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        return CompletableFuture.completedFuture(connector.update(objectClass, uid, attrs, options));
    }

    @Async
    public Future<Set<AttributeDelta>> updateDelta(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final Set<AttributeDelta> modifications,
            final OperationOptions options) {

        return CompletableFuture.completedFuture(connector.updateDelta(objectClass, uid, modifications, options));
    }

    @Async
    public Future<Uid> delete(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        connector.delete(objectClass, uid, options);
        return CompletableFuture.completedFuture(uid);
    }

    @Async
    public Future<SyncToken> getLatestSyncToken(
            final ConnectorFacade connector, final ObjectClass objectClass) {

        return CompletableFuture.completedFuture(connector.getLatestSyncToken(objectClass));
    }

    @Async
    public Future<ConnectorObject> getObject(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Attribute connObjectKey,
            final boolean ignoreCaseMatch,
            final OperationOptions options) {

        ConnectorObject[] objects = new ConnectorObject[1];
        connector.search(
                objectClass,
                ignoreCaseMatch ? FilterBuilder.equalsIgnoreCase(connObjectKey) : FilterBuilder.equalTo(connObjectKey),
                new SearchResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject connectorObject) {
                objects[0] = connectorObject;
                return false;
            }

            @Override
            public void handleResult(final SearchResult sr) {
                // do nothing
            }
        },
                options);

        return CompletableFuture.completedFuture(objects[0]);
    }

    @Async
    public Future<Set<ObjectClassInfo>> getObjectClassInfo(final ConnectorFacade connector) {
        Set<ObjectClassInfo> result = Set.of();

        try {
            result = connector.schema().getObjectClassInfo();
        } catch (Exception e) {
            // catch exception in order to manage unpredictable behaviors
            LOG.debug("While reading schema on connector {}", connector, e);
        }

        return CompletableFuture.completedFuture(result);
    }

    @Async
    public Future<String> validate(final ConnectorFacade connector) {
        connector.validate();
        return CompletableFuture.completedFuture("OK");
    }

    @Async
    public Future<String> test(final ConnectorFacade connector) {
        connector.test();
        return CompletableFuture.completedFuture("OK");
    }
}
