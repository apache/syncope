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
import io.swagger.v3.oas.annotations.media.Schema;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;

@XmlRootElement(name = "propagationTask")
@XmlType
public class PropagationTaskTO extends TaskTO {

    private static final long serialVersionUID = 386450127003321197L;

    private ResourceOperation operation;

    private String connObjectKey;

    private String oldConnObjectKey;

    private String attributes;

    private String resource;

    private String objectClassName;

    private AnyTypeKind anyTypeKind;

    private String anyType;

    private String entityKey;

    @XmlTransient
    @JsonProperty("@class")
    @Schema(name = "@class", required = true, example = "org.apache.syncope.common.lib.to.PropagationTaskTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
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
    @XmlElement(required = true)
    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public ResourceOperation getOperation() {
        return operation;
    }

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(final String attributes) {
        this.attributes = attributes;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(final String objectClassName) {
        this.objectClassName = objectClassName;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public String getAnyType() {
        return anyType;
    }

    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }
}
