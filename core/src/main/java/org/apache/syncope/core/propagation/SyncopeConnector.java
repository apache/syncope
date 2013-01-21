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
package org.apache.syncope.core.propagation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;

public interface SyncopeConnector {

    /**
     * Create user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param attrs attributes for creation
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return Uid for created user
     */
    Uid create(PropagationMode propagationMode, ObjectClass objectClass,
            Set<Attribute> attrs, OperationOptions options,
            Set<String> propagationAttempted);

    /**
     * Update user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param uid user to be updated
     * @param attrs attributes for update
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if update is actually performed (based on connector instance's capabilities)
     * @return Uid for created user
     */
    Uid update(PropagationMode propagationMode, ObjectClass objectClass,
            Uid uid, Set<Attribute> attrs, OperationOptions options,
            Set<String> propagationAttempted);

    /**
     * Delete user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param uid user to be deleted
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if deletion is actually performed (based on connector instance's capabilities)
     */
    void delete(PropagationMode propagationMode, ObjectClass objectClass,
            Uid uid, OperationOptions options, Set<String> propagationAttempted);

    /**
     * Sync users from a connector instance.
     *
     * @param objectClass ConnId's object class.
     * @param token to be passed to the underlying connector
     * @param handler to be used to handle deltas.
     */
    void sync(ObjectClass objectClass, SyncToken token,
            SyncResultsHandler handler, OperationOptions options);

    /**
     * Read latest sync token from a connector instance.
     *
     * @param objectClass ConnId's object class.
     * @return latest sync token
     */
    SyncToken getLatestSyncToken(ObjectClass objectClass);

    /**
     * Get remote object.
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    ConnectorObject getObject(ObjectClass objectClass, Uid uid,
            OperationOptions options);

    /**
     * Get remote object used by the propagation manager in order to choose for a create (object doesn't exist) or an
     * update (object exists).
     *
     * @param propagationMode propagation mode
     * @param operationType resource operation type
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    ConnectorObject getObject(PropagationMode propagationMode,
            ResourceOperation operationType, ObjectClass objectClass, Uid uid,
            OperationOptions options);

    List<ConnectorObject> search(ObjectClass objectClass, Filter filter,
            OperationOptions options);

    /**
     * Get remote object used by the propagation manager in order to choose for a create (object doesn't exist) or an
     * update (object exists).
     *
     * @param objectClass ConnId's object class.
     * @param handler to be used to handle deltas.
     * @param options ConnId's OperationOptions.
     */
    void getAllObjects(ObjectClass objectClass, SyncResultsHandler handler,
            OperationOptions options);

    /**
     * Read attribute for a given connector object.
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @param attributeName attribute to read
     * @return attribute (if present)
     */
    Attribute getObjectAttribute(ObjectClass objectClass, Uid uid,
            OperationOptions options, String attributeName);

    /**
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return attributes (if present)
     */
    Set<Attribute> getObjectAttributes(ObjectClass objectClass, Uid uid,
            OperationOptions options);

    /**
     * Return resource schema names.
     *
     * @param showall return special attributes (like as __NAME__ or __PASSWORD__) if true
     * @return a list of schema names
     */
    Set<String> getSchema(boolean showall);

    /**
     * Validate a connector instance.
     */
    void validate();

    /**
     * Check connection to resource.
     */
    void test();

    /**
     * Getter for active connector instance.
     *
     * @return active connector instance.
     */
    ConnInstance getActiveConnInstance();

    OperationOptions getOperationOptions(
            Collection<AbstractMappingItem> mapItems);

    String toString();

}