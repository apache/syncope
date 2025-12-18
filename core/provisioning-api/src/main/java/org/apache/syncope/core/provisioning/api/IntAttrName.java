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
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.entity.Schema;

public class IntAttrName {

    public record SchemaInfo(Schema schema, SchemaType type) {

    }

    public record RelationshipInfo(String type, String anyObject) {

    }

    protected static class Builder {

        private final IntAttrName instance = new IntAttrName();

        public Builder withField(final String field) {
            instance.field = field;
            return this;
        }

        public Builder withSchemaInfo(final SchemaInfo schemaInfo) {
            instance.schemaInfo = schemaInfo;
            return this;
        }

        public Builder withExternalGroup(final String externalGroup) {
            instance.externalGroup = externalGroup;
            return this;
        }

        public Builder withExternalUser(final String externalUser) {
            instance.externalUser = externalUser;
            return this;
        }

        public Builder withExternalAnyObject(final String externalAnyObject) {
            instance.externalAnyObject = externalAnyObject;
            return this;
        }

        public Builder withMembership(final String membership) {
            instance.membership = membership;
            return this;
        }

        public Builder withRelationship(final String type, final String anyObject) {
            instance.relationshipInfo = new RelationshipInfo(type, anyObject);
            return this;
        }

        protected IntAttrName build() {
            return instance;
        }
    }

    private String field;

    private SchemaInfo schemaInfo;

    private String externalGroup;

    private String externalUser;

    private String externalAnyObject;

    private String membership;

    private RelationshipInfo relationshipInfo;

    public String getField() {
        return field;
    }

    public SchemaInfo getSchemaInfo() {
        return schemaInfo;
    }

    public String getExternalGroup() {
        return externalGroup;
    }

    public String getExternalUser() {
        return externalUser;
    }

    public String getExternalAnyObject() {
        return externalAnyObject;
    }

    public String getMembership() {
        return membership;
    }

    public RelationshipInfo getRelationshipInfo() {
        return relationshipInfo;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(field).
                append(schemaInfo).
                append(externalGroup).
                append(externalUser).
                append(externalAnyObject).
                append(membership).
                append(relationshipInfo).
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
                append(field, other.field).
                append(schemaInfo, other.schemaInfo).
                append(externalGroup, other.externalGroup).
                append(externalUser, other.externalUser).
                append(externalAnyObject, other.externalAnyObject).
                append(membership, other.membership).
                append(relationshipInfo, other.relationshipInfo).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(field).
                append(schemaInfo).
                append(externalGroup).
                append(externalUser).
                append(externalAnyObject).
                append(membership).
                append(relationshipInfo).
                build();
    }
}
