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
package org.apache.syncope.core.persistence.jpa.entity.task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.jpa.validation.entity.PropagationTaskCheck;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.identityconnectors.framework.common.objects.Attribute;

/**
 * Encapsulate all information about a propagation task.
 */
@Entity
@PropagationTaskCheck
@Table(name = JPAPropagationTask.TABLE)
public class JPAPropagationTask extends AbstractTask implements PropagationTask {

    private static final long serialVersionUID = 7086054884614511210L;
    
    public static final String TABLE = "PropagationTask";

    /**
     * @see ResourceOperation
     */
    @Enumerated(EnumType.STRING)
    private ResourceOperation operation;

    /**
     * The connObjectKey on the external resource.
     */
    private String connObjectKey;

    /**
     * The (optional) former connObjectKey on the external resource.
     */
    private String oldConnObjectKey;

    /**
     * Attributes to be propagated.
     */
    @Lob
    private String attributes;

    private String objectClassName;

    @Enumerated(EnumType.STRING)
    private AnyTypeKind anyTypeKind;

    private String anyType;

    private String entityKey;

    /**
     * ExternalResource to which the propagation happens.
     */
    @ManyToOne
    private JPAExternalResource resource;

    @Override
    public String getConnObjectKey() {
        return connObjectKey;
    }

    @Override
    public void setConnObjectKey(final String connObjectKey) {
        this.connObjectKey = connObjectKey;
    }

    @Override
    public String getOldConnObjectKey() {
        return oldConnObjectKey;
    }

    @Override
    public void setOldConnObjectKey(final String oldConnObjectKey) {
        this.oldConnObjectKey = oldConnObjectKey;
    }

    @Override
    public String getSerializedAttributes() {
        return this.attributes;
    }

    @Override
    public Set<Attribute> getAttributes() {
        Set<Attribute> result = new HashSet<>();
        if (StringUtils.isNotBlank(this.attributes)) {
            result.addAll(Arrays.asList(POJOHelper.deserialize(this.attributes, Attribute[].class)));
        }

        return result;
    }

    @Override
    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = POJOHelper.serialize(attributes);
    }

    @Override

    public ResourceOperation getOperation() {
        return operation;
    }

    @Override

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        this.resource = (JPAExternalResource) resource;
    }

    @Override
    public String getObjectClassName() {
        return objectClassName;
    }

    @Override
    public void setObjectClassName(final String objectClassName) {
        this.objectClassName = objectClassName;
    }

    @Override
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Override
    public String getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    @Override
    public String getEntityKey() {
        return entityKey;
    }

    @Override
    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }
}
