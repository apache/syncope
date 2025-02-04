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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.entity.Schema;

public class IntAttrName {

    private AnyTypeKind anyTypeKind;

    private String field;

    private SchemaType schemaType;

    private Schema schema;

    private String enclosingGroup;

    private String relatedUser;

    private String relatedAnyObject;

    private String membershipOfGroup;

    private String relationshipType;

    private String relationshipAnyType;

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(final SchemaType schemaType) {
        this.schemaType = schemaType;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(final Schema schemaName) {
        this.schema = schemaName;
    }

    public String getEnclosingGroup() {
        return enclosingGroup;
    }

    public void setEnclosingGroup(final String enclosingGroup) {
        this.enclosingGroup = enclosingGroup;
    }

    public String getRelatedUser() {
        return relatedUser;
    }

    public void setRelatedUser(final String relatedUser) {
        this.relatedUser = relatedUser;
    }

    public String getRelatedAnyObject() {
        return relatedAnyObject;
    }

    public void setRelatedAnyObject(final String relatedAnyObject) {
        this.relatedAnyObject = relatedAnyObject;
    }

    public String getMembershipOfGroup() {
        return membershipOfGroup;
    }

    public void setMembershipOfGroup(final String membershipOfGroup) {
        this.membershipOfGroup = membershipOfGroup;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(final String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getRelationshipAnyType() {
        return relationshipAnyType;
    }

    public void setRelationshipAnyType(final String relationshipAnyType) {
        this.relationshipAnyType = relationshipAnyType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(anyTypeKind).
                append(field).
                append(schemaType).
                append(schema).
                append(enclosingGroup).
                append(relatedUser).
                append(relatedAnyObject).
                append(membershipOfGroup).
                append(relationshipType).
                append(relationshipAnyType).
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
        final IntAttrName other = (IntAttrName) obj;
        return new EqualsBuilder().
                append(anyTypeKind, other.anyTypeKind).
                append(field, other.field).
                append(schemaType, other.schemaType).
                append(schema, other.schema).
                append(enclosingGroup, other.enclosingGroup).
                append(relatedUser, other.relatedUser).
                append(relatedAnyObject, other.relatedAnyObject).
                append(membershipOfGroup, other.membershipOfGroup).
                append(relationshipType, other.relationshipType).
                append(relationshipAnyType, other.relationshipAnyType).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(anyTypeKind).
                append(field).
                append(schemaType).
                append(schema).
                append(enclosingGroup).
                append(relatedUser).
                append(relatedAnyObject).
                append(membershipOfGroup).
                append(relationshipType).
                append(relationshipAnyType).
                build();
    }
}
