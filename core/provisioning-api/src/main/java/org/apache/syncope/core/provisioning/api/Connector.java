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
package org.apache.syncope.core.provisioning.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.LiveSyncResultsHandler;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.springframework.data.domain.Sort;

/**
 * Entry point for making requests on underlying connector bundles.
 */
public interface Connector {

    /**
     * Authenticate user on a connector instance.
     *
     * @param username the name based credential for authentication
     * @param password the password based credential for authentication
     * @param options ConnId's OperationOptions
     * @return Uid of the account that was used to authenticate
     */
    Uid authenticate(String username, String password, OperationOptions options);

    /**
     * Create user, group or any object on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param attrs attributes for creation
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return Uid for created object
     */
    Uid create(
            ObjectClass objectClass,
            Set<Attribute> attrs,
            OperationOptions options,
            Mutable<Boolean> propagationAttempted);

    /**
     * Update user, group or any object on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param uid remote identifier
     * @param attrs attributes for update
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return Uid for updated object
     */
    Uid update(
            ObjectClass objectClass,
            Uid uid,
            Set<Attribute> attrs,
            OperationOptions options,
            Mutable<Boolean> propagationAttempted);

    /**
     * Partial update user, group or any object on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param uid remote identifier
     * @param modifications attribute modifications to apply
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return the applied modifications
     */
    Set<AttributeDelta> updateDelta(
            ObjectClass objectClass,
            Uid uid,
            Set<AttributeDelta> modifications,
            OperationOptions options,
            Mutable<Boolean> propagationAttempted);

    /**
     * Delete user, group or any object on a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param uid user to be deleted
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if deletion is actually performed (based on connector instance's capabilities)
     */
    void delete(
            ObjectClass objectClass,
            Uid uid,
            OperationOptions options,
            Mutable<Boolean> propagationAttempted);

    /**
     * Fetches all remote objects (for use during full reconciliation).
     *
     * @param objectClass ConnId's object class.
     * @param handler to be used to handle deltas.
     * @param options ConnId's OperationOptions.
     */
    default void fullReconciliation(ObjectClass objectClass, SyncResultsHandler handler, OperationOptions options) {
        filteredReconciliation(objectClass, null, handler, options);
    }

    /**
     * Fetches remote objects (for use during filtered reconciliation).
     *
     * @param objectClass ConnId's object class.
     * @param filterBuilder reconciliation filter builder
     * @param handler to be used to handle deltas.
     * @param options ConnId's OperationOptions.
     */
    default void filteredReconciliation(
            ObjectClass objectClass,
            ReconFilterBuilder filterBuilder,
            SyncResultsHandler handler,
            OperationOptions options) {

        Filter filter = null;
        OperationOptions actualOptions = options;
        if (filterBuilder != null) {
            filter = filterBuilder.build(objectClass);
            actualOptions = filterBuilder.build(objectClass, actualOptions);
        }

        search(objectClass, filter, new SearchResultsHandler() {

            @Override
            public void handleResult(final SearchResult result) {
                // nothing to do
            }

            @Override
            public boolean handle(final ConnectorObject object) {
                return handler.handle(new SyncDeltaBuilder().
                        setObject(object).
                        setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                        setToken(new SyncToken("")).
                        build());
            }
        }, actualOptions);
    }

    /**
     * Sync remote objects from a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param token to be passed to the underlying connector
     * @param handler to be used to handle deltas
     * @param options ConnId's OperationOptions
     */
    void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options);

    /**
     * Read latest sync token from a connector instance.
     *
     * @param objectClass ConnId's object class.
     * @return latest sync token
     */
    SyncToken getLatestSyncToken(ObjectClass objectClass);

    /**
     * Live sync remote objects from a connector instance.
     *
     * @param objectClass ConnId's object class
     * @param handler to be used to handle deltas
     * @param options ConnId's OperationOptions
     */
    void livesync(ObjectClass objectClass, LiveSyncResultsHandler handler, OperationOptions options);

    /**
     * Get remote object.
     *
     * @param objectClass ConnId's object class
     * @param connObjectKey ConnId's key attribute
     * @param ignoreCaseMatch whether match should be performed regardless of the value case
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    ConnectorObject getObject(
            ObjectClass objectClass,
            Attribute connObjectKey,
            boolean ignoreCaseMatch,
            OperationOptions options);

    /**
     * Search for remote objects.
     *
     * @param objectClass ConnId's object class
     * @param filter search filter
     * @param handler class responsible for working with the objects returned from the search; may be null.
     * @param options ConnId's OperationOptions
     * @return search result
     */
    SearchResult search(
            ObjectClass objectClass,
            Filter filter,
            SearchResultsHandler handler,
            OperationOptions options);

    /**
     * Search for remote objects.
     *
     * @param objectClass ConnId's object class
     * @param filter search filter
     * @param handler class responsible for working with the objects returned from the search; may be null.
     * @param pageSize requested page results page size
     * @param pagedResultsCookie an opaque cookie which is used by the connector to track its position in the set of
     * query results
     * @param sort the sort keys which should be used for ordering the {@link ConnectorObject} returned by
     * search request
     * @param options ConnId's OperationOptions
     * @return search result
     */
    default SearchResult search(
            ObjectClass objectClass,
            Filter filter,
            SearchResultsHandler handler,
            int pageSize,
            String pagedResultsCookie,
            List<Sort.Order> sort,
            OperationOptions options) {

        OperationOptionsBuilder builder = new OperationOptionsBuilder().setPageSize(pageSize).setPagedResultsOffset(-1);
        Optional.ofNullable(pagedResultsCookie).ifPresent(builder::setPagedResultsCookie);
        builder.setSortKeys(sort.stream().
                map(clause -> new SortKey(clause.getProperty(), clause.getDirection() == Sort.Direction.ASC)).
                toList());
        builder.setAttributesToGet(options.getAttributesToGet());

        return search(objectClass, filter, handler, builder.build());
    }

    /**
     * Builds metadata description of ConnId {@link ObjectClass}.
     *
     * @return metadata description of ConnId ObjectClass
     */
    Set<ObjectClassInfo> getObjectClassInfo();

    /**
     * Validate connector instance.
     */
    void validate();

    /**
     * Check connection.
     */
    void test();

    /**
     * Dispose of any resources associated with connector instance.
     */
    void dispose();

    /**
     * Getter for active connector instance.
     *
     * @return active connector instance.
     */
    ConnInstance getConnInstance();
}
