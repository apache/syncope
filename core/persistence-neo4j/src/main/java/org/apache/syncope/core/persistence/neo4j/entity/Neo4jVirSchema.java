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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jVirSchema.NODE)
public class Neo4jVirSchema extends Neo4jSchema implements VirSchema {

    private static final long serialVersionUID = -4136127093718028088L;

    public static final String NODE = "VirSchema";

    public static final String VIRSCHEMA_RESOURCE_REL = "VIRSCHEMA_RESOURCE";

    public static final String VIRSCHEMA_ANYTYPE_REL = "VIRSCHEMA_ANYTYPE";

    @NotNull
    private String extAttrName;

    private Boolean readonly = false;

    @Relationship(type = Neo4jAnyTypeClass.ANY_TYPE_CLASS_VIR_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyTypeClass anyTypeClass;

    @NotNull
    @Relationship(type = VIRSCHEMA_RESOURCE_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jExternalResource resource;

    @NotNull
    @Relationship(type = VIRSCHEMA_ANYTYPE_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType anyType;

    @Override
    public String getExtAttrName() {
        return extAttrName;
    }

    @Override
    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

    @Override
    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    @Override
    public AttrSchemaType getType() {
        return AttrSchemaType.String;
    }

    @Override
    public String getMandatoryCondition() {
        return Boolean.FALSE.toString().toLowerCase();
    }

    @Override
    public boolean isMultivalue() {
        return true;
    }

    @Override
    public boolean isUniqueConstraint() {
        return false;
    }

    @Override
    public AnyTypeClass getAnyTypeClass() {
        return anyTypeClass;
    }

    @Override
    public void setAnyTypeClass(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, Neo4jAnyTypeClass.class);
        this.anyTypeClass = (Neo4jAnyTypeClass) anyTypeClass;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        this.resource = (Neo4jExternalResource) resource;
    }

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.anyType = (Neo4jAnyType) anyType;
    }
}
