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
package org.apache.syncope.core.propagation.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 * Intercept calls to ConnectorFacade's methods and check if the corresponding connector instance has been configured to
 * allow every single operation: if not, simply do nothing.
 */
public class AsyncConnectorFacade {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AsyncConnectorFacade.class);

    @Async
    public Future<Uid> create(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        return new AsyncResult<Uid>(connector.create(objectClass, attrs, options));
    }

    @Async
    public Future<Uid> update(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        return new AsyncResult<Uid>(connector.update(objectClass, uid, attrs, options));
    }

    @Async
    public Future<Uid> delete(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        connector.delete(objectClass, uid, options);
        return new AsyncResult<Uid>(uid);
    }

    @Async
    public Future<SyncToken> getLatestSyncToken(
            final ConnectorFacade connector, final ObjectClass objectClass) {

        return new AsyncResult<SyncToken>(connector.getLatestSyncToken(objectClass));
    }

    @Async
    public Future<ConnectorObject> getObject(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        return new AsyncResult<ConnectorObject>(connector.getObject(objectClass, uid, options));
    }

    @Async
    public Future<Attribute> getObjectAttribute(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final String attributeName) {

        Attribute attribute = null;

        final ConnectorObject object = connector.getObject(objectClass, uid, options);
        if (object == null) {
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        } else {
            attribute = object.getAttributeByName(attributeName);
        }

        return new AsyncResult<Attribute>(attribute);
    }

    @Async
    public Future<Set<Attribute>> getObjectAttributes(
            final ConnectorFacade connector,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        final Set<Attribute> attributes = new HashSet<Attribute>();

        final ConnectorObject object = connector.getObject(objectClass, uid, options);

        if (object == null) {
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        } else {
            for (String attribute : options.getAttributesToGet()) {
                attributes.add(object.getAttributeByName(attribute));
            }
        }

        return new AsyncResult<Set<Attribute>>(attributes);
    }

    @Async
    public Future<Set<String>> getSchemaNames(final ConnectorFacade connector, final boolean includeSpecial) {
        final Set<String> schemaNames = new HashSet<String>();

        try {
            final Schema schema = connector.schema();
            for (ObjectClassInfo info : schema.getObjectClassInfo()) {
                for (AttributeInfo attrInfo : info.getAttributeInfo()) {
                    if (includeSpecial || !AttributeUtil.isSpecialName(attrInfo.getName())) {
                        schemaNames.add(attrInfo.getName());
                    }
                }
            }
        } catch (Exception e) {
            // catch exception in order to manage unpredictable behaviors
            LOG.debug("While reading schema on connector {}", connector, e);
        }

        return new AsyncResult<Set<String>>(schemaNames);
    }

    @Async
    public Future<Set<ObjectClass>> getSupportedObjectClasses(final ConnectorFacade connector) {
        final Set<ObjectClass> objectClasses = new HashSet<ObjectClass>();

        try {
            final Schema schema = connector.schema();
            for (ObjectClassInfo info : schema.getObjectClassInfo()) {
                objectClasses.add(new ObjectClass(info.getType()));
            }
        } catch (Exception e) {
            // catch exception in order to manage unpredictable behaviors
            LOG.debug("While reading schema on connector {}", connector, e);
        }

        return new AsyncResult<Set<ObjectClass>>(objectClasses);
    }

    @Async
    public Future<String> validate(final ConnectorFacade connector) {
        connector.validate();
        return new AsyncResult<String>("OK");
    }

    @Async
    public Future<String> test(final ConnectorFacade connector) {
        connector.test();
        return new AsyncResult<String>("OK");
    }
}
