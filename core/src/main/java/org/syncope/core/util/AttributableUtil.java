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
package org.syncope.core.util;

import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.beans.membership.MAttr;
import org.syncope.core.persistence.beans.membership.MAttrUniqueValue;
import org.syncope.core.persistence.beans.membership.MAttrValue;
import org.syncope.core.persistence.beans.membership.MDerAttr;
import org.syncope.core.persistence.beans.membership.MDerSchema;
import org.syncope.core.persistence.beans.membership.MSchema;
import org.syncope.core.persistence.beans.membership.MVirAttr;
import org.syncope.core.persistence.beans.membership.MVirSchema;
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.beans.role.RAttrUniqueValue;
import org.syncope.core.persistence.beans.role.RAttrValue;
import org.syncope.core.persistence.beans.role.RDerAttr;
import org.syncope.core.persistence.beans.role.RDerSchema;
import org.syncope.core.persistence.beans.role.RSchema;
import org.syncope.core.persistence.beans.role.RVirAttr;
import org.syncope.core.persistence.beans.role.RVirSchema;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirAttr;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.types.AttributableType;
import org.syncope.types.IntMappingType;

public class AttributableUtil {

    private final AttributableType type;

    public static AttributableUtil getInstance(final AttributableType type) {
        return new AttributableUtil(type);
    }

    public static AttributableUtil valueOf(final String name) {
        return new AttributableUtil(AttributableType.valueOf(name));
    }

    private AttributableUtil(final AttributableType type) {
        this.type = type;
    }

    public AttributableType getType() {
        return type;
    }

    public IntMappingType intMappingType() {
        IntMappingType result;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleSchema;
                break;
            case MEMBERSHIP:
                result = IntMappingType.MembershipSchema;
                break;
            case USER:
            default:
                result = IntMappingType.UserSchema;
                break;
        }

        return result;
    }

    public IntMappingType derivedIntMappingType() {
        IntMappingType result;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleDerivedSchema;
                break;
            case MEMBERSHIP:
                result = IntMappingType.MembershipDerivedSchema;
                break;
            case USER:
            default:
                result = IntMappingType.UserDerivedSchema;
                break;
        }

        return result;
    }

    public IntMappingType virtualIntMappingType() {
        IntMappingType result;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleVirtualSchema;
                break;
            case MEMBERSHIP:
                result = IntMappingType.MembershipVirtualSchema;
                break;
            case USER:
            default:
                result = IntMappingType.UserVirtualSchema;
                break;
        }

        return result;
    }

    public <T extends AbstractSchema> Class<T> schemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = USchema.class;
                break;
            case ROLE:
                result = RSchema.class;
                break;
            case MEMBERSHIP:
                result = MSchema.class;
                break;
        }

        return result;
    }

    public <T extends AbstractSchema> T newSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new USchema();
                break;
            case ROLE:
                result = (T) new RSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MSchema();
                break;
        }

        return result;
    }

    public <T extends AbstractDerSchema> Class<T> derivedSchemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UDerSchema.class;
                break;
            case ROLE:
                result = RDerSchema.class;
                break;
            case MEMBERSHIP:
                result = MDerSchema.class;
                break;
        }

        return result;
    }

    public <T extends AbstractVirSchema> Class<T> virtualSchemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UVirSchema.class;
                break;
            case ROLE:
                result = RVirSchema.class;
                break;
            case MEMBERSHIP:
                result = MVirSchema.class;
                break;
        }

        return result;
    }

    public <T extends AbstractDerSchema> T newDerivedSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UDerSchema();
                break;
            case ROLE:
                result = (T) new RDerSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MDerSchema();
                break;
        }

        return result;
    }

    public <T extends AbstractAttr> Class<T> attributeClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UAttr.class;
                break;
            case ROLE:
                result = RAttr.class;
                break;
            case MEMBERSHIP:
                result = MAttr.class;
                break;
        }

        return result;
    }

    public <T extends AbstractAttr> T newAttribute() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UAttr();
                break;
            case ROLE:
                result = (T) new RAttr();
                break;
            case MEMBERSHIP:
                result = (T) new MAttr();
                break;
        }

        return result;
    }

    public <T extends AbstractDerAttr> Class<T> derivedAttributeClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UDerAttr.class;
                break;
            case ROLE:
                result = RDerAttr.class;
                break;
            case MEMBERSHIP:
                result = MDerAttr.class;
                break;
        }

        return result;
    }

    public <T extends AbstractVirAttr> Class<T> virtualAttributeClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UVirAttr.class;
                break;
            case ROLE:
                result = RVirAttr.class;
                break;
            case MEMBERSHIP:
                result = MVirAttr.class;
                break;
        }

        return result;
    }

    public <T extends AbstractDerAttr> T newDerivedAttribute() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UDerAttr();
                break;
            case ROLE:
                result = (T) new RDerAttr();
                break;
            case MEMBERSHIP:
                result = (T) new MDerAttr();
                break;
        }

        return result;
    }

    public <T extends AbstractVirAttr> T newVirtualAttribute() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UVirAttr();
                break;
            case ROLE:
                result = (T) new RVirAttr();
                break;
            case MEMBERSHIP:
                result = (T) new MVirAttr();
                break;
        }

        return result;
    }

    public <T extends AbstractVirSchema> T newVirtualSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UVirSchema();
                break;
            case ROLE:
                result = (T) new RVirSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MVirSchema();
                break;
        }

        return result;
    }

    public <T extends AbstractAttrValue> Class<T> attributeValueClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = UAttrValue.class;
                break;
            case ROLE:
                result = RAttrValue.class;
                break;
            case MEMBERSHIP:
                result = MAttrValue.class;
                break;
        }

        return result;
    }

    public <T extends AbstractAttrValue> T newAttributeValue() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UAttrValue();
                break;
            case ROLE:
                result = (T) new RAttrValue();
                break;
            case MEMBERSHIP:
                result = (T) new MAttrValue();
                break;
        }

        return result;
    }

    public <T extends AbstractAttrValue> T newAttributeUniqueValue() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UAttrUniqueValue();
                break;
            case ROLE:
                result = (T) new RAttrUniqueValue();
                break;
            case MEMBERSHIP:
                result = (T) new MAttrUniqueValue();
                break;
        }

        return result;
    }
}
