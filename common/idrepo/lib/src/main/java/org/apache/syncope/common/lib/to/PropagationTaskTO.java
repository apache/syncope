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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;

@Schema(allOf = { TaskTO.class })
public class PropagationTaskTO extends TaskTO {

    private static final long serialVersionUID = 386450127003321197L;

    private ResourceOperation operation;

    private String connObjectKey;

    private String oldConnObjectKey;

    private String propagationData;

    private String resource;

    private String objectClassName;

    private AnyTypeKind anyTypeKind;

    private String anyType;

    private String entityKey;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.PropagationTaskTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    @JsonProperty(required = true)
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

    @JsonProperty(required = true)
    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    @JsonProperty(required = true)
    public ResourceOperation getOperation() {
        return operation;
    }

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @JsonProperty(required = true)
    public String getPropagationData() {
        return propagationData;
    }

    public void setPropagationData(final String propagationData) {
        this.propagationData = propagationData;
    }

    @JsonProperty(required = true)
    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(final String objectClassName) {
        this.objectClassName = objectClassName;
    }

    @JsonProperty(required = true)
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @JsonProperty(required = true)
    public String getAnyType() {
        return anyType;
    }

    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    @JsonProperty(required = true)
    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(operation).
                append(connObjectKey).
                append(oldConnObjectKey).
                append(propagationData).
                append(resource).
                append(objectClassName).
                append(anyTypeKind).
                append(anyType).
                append(entityKey).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PropagationTaskTO other = (PropagationTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(operation, other.operation).
                append(connObjectKey, other.connObjectKey).
                append(oldConnObjectKey, other.oldConnObjectKey).
                append(propagationData, other.propagationData).
                append(resource, other.resource).
                append(objectClassName, other.objectClassName).
                append(anyTypeKind, other.anyTypeKind).
                append(anyType, other.anyType).
                append(entityKey, other.entityKey).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
                appendSuper(super.toString()).
                append(operation).
                append(connObjectKey).
                append(oldConnObjectKey).
                append(propagationData).
                append(resource).
                append(objectClassName).
                append(anyTypeKind).
                append(anyType).
                append(entityKey).
                build();
    }
}
