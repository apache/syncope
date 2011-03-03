/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.util;

import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.membership.MAttr;
import org.syncope.core.persistence.beans.membership.MAttrUniqueValue;
import org.syncope.core.persistence.beans.membership.MAttrValue;
import org.syncope.core.persistence.beans.membership.MDerAttr;
import org.syncope.core.persistence.beans.membership.MDerSchema;
import org.syncope.core.persistence.beans.membership.MSchema;
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.beans.role.RAttrUniqueValue;
import org.syncope.core.persistence.beans.role.RAttrValue;
import org.syncope.core.persistence.beans.role.RDerAttr;
import org.syncope.core.persistence.beans.role.RDerSchema;
import org.syncope.core.persistence.beans.role.RSchema;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.types.SourceMappingType;

public enum AttributableUtil {

    USER, ROLE, MEMBERSHIP;

    public SourceMappingType sourceMappingType() {
        SourceMappingType result = null;

        switch (this) {
            case USER:
                result = SourceMappingType.UserSchema;
                break;
            case ROLE:
                result = SourceMappingType.RoleSchema;
                break;
            case MEMBERSHIP:
                result = SourceMappingType.MembershipSchema;
                break;
        }

        return result;
    }

    public <T extends AbstractSchema> Class<T> schemaClass() {
        Class result = null;

        switch (this) {
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

        switch (this) {
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

        switch (this) {
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

    public <T extends AbstractDerSchema> T newDerivedSchema() {
        T result = null;

        switch (this) {
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

        switch (this) {
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

        switch (this) {
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

        switch (this) {
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

    public <T extends AbstractDerAttr> T newDerivedAttribute() {
        T result = null;

        switch (this) {
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

    public <T extends AbstractAttrValue> Class<T> attributeValueClass() {
        Class result = null;

        switch (this) {
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

        switch (this) {
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

        switch (this) {
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
