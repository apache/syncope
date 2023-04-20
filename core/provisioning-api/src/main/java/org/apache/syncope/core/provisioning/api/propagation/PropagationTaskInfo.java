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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Optional;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.provisioning.api.Connector;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;

public class PropagationTaskInfo {

    private String key;

    private final ExternalResource resource;

    private final ResourceOperation operation;

    private final ObjectClass objectClass;

    private final AnyTypeKind anyTypeKind;

    private final String anyType;

    private final String entityKey;

    private String connObjectKey;

    private String oldConnObjectKey;

    private final PropagationData propagationData;

    private Connector connector;

    private Optional<ConnectorObject> beforeObj = Optional.empty();

    private AnyUR updateRequest;

    public PropagationTaskInfo(
            final ExternalResource resource,
            final ResourceOperation operation,
            final ObjectClass objectClass,
            final AnyTypeKind anyTypeKind,
            final String anyType,
            final String entityKey,
            final String connObjectKey,
            final PropagationData propagationData) {

        this.resource = resource;
        this.operation = operation;
        this.objectClass = objectClass;
        this.anyTypeKind = anyTypeKind;
        this.anyType = anyType;
        this.entityKey = entityKey;
        this.connObjectKey = connObjectKey;
        this.propagationData = propagationData;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public ExternalResource getResource() {
        return resource;
    }

    public ResourceOperation getOperation() {
        return operation;
    }

    public ObjectClass getObjectClass() {
        return objectClass;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public String getAnyType() {
        return anyType;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public String getConnObjectKey() {
        return connObjectKey;
    }

    public void setConnObjectKey(final String connObjectKey) {
        this.connObjectKey = connObjectKey;
    }

    public String getOldConnObjectKey() {
        return oldConnObjectKey;
    }

    public void setOldConnObjectKey(final String oldConnObjectKey) {
        this.oldConnObjectKey = oldConnObjectKey;
    }

    public PropagationData getPropagationData() {
        return propagationData;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(final Connector connector) {
        this.connector = connector;
    }

    public Optional<ConnectorObject> getBeforeObj() {
        return beforeObj;
    }

    public void setBeforeObj(final Optional<ConnectorObject> beforeObj) {
        this.beforeObj = beforeObj;
    }

    public AnyUR getUpdateRequest() {
        return updateRequest;
    }

    public void setUpdateRequest(final AnyUR updateRequest) {
        this.updateRequest = updateRequest;
    }

    @Override
    public String toString() {
        return "PropagationTaskInfo{"
                + "key=" + key
                + ",resource=" + resource.getKey()
                + ", operation=" + operation
                + ", objectClass=" + objectClass
                + ", anyTypeKind=" + anyTypeKind
                + ", anyType=" + anyType
                + ", entityKey=" + entityKey
                + ", connObjectKey=" + connObjectKey
                + ", oldConnObjectKey=" + oldConnObjectKey
                + ", propagationData=" + propagationData
                + ", connector=" + connector
                + ", beforeObj=" + beforeObj
                + ", updateRequest=" + updateRequest
                + '}';
    }
}
