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
package org.syncope.core.rest.data;

import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.membership.MembershipAttribute;
import org.syncope.core.persistence.beans.membership.MembershipAttributeValue;
import org.syncope.core.persistence.beans.membership.MembershipDerivedAttribute;
import org.syncope.core.persistence.beans.membership.MembershipDerivedSchema;
import org.syncope.core.persistence.beans.membership.MembershipSchema;
import org.syncope.core.persistence.beans.role.RoleAttribute;
import org.syncope.core.persistence.beans.role.RoleAttributeValue;
import org.syncope.core.persistence.beans.role.RoleDerivedAttribute;
import org.syncope.core.persistence.beans.role.RoleDerivedSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserDerivedAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;

public enum AttributableUtil {

    USER, ROLE, MEMBERSHIP;

    public <T extends AbstractSchema> Class<T> getSchemaClass() {
        Class result = null;

        switch (this) {
            case USER:
                result = UserSchema.class;
                break;
            case ROLE:
                result = RoleSchema.class;
                break;
            case MEMBERSHIP:
                result = MembershipSchema.class;
                break;
        }

        return result;
    }

    public <T extends AbstractSchema> T newSchema() {
        T result = null;

        switch (this) {
            case USER:
                result = (T) new UserSchema();
                break;
            case ROLE:
                result = (T) new RoleSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipSchema();
                break;
        }

        return result;
    }

    public <T extends AbstractDerivedSchema> Class<T> getDerivedSchemaClass() {
        Class result = null;

        switch (this) {
            case USER:
                result = UserDerivedSchema.class;
                break;
            case ROLE:
                result = RoleDerivedSchema.class;
                break;
            case MEMBERSHIP:
                result = MembershipDerivedSchema.class;
                break;
        }

        return result;
    }

    public <T extends AbstractDerivedSchema> T newDerivedSchema() {
        T result = null;

        switch (this) {
            case USER:
                result = (T) new UserDerivedSchema();
                break;
            case ROLE:
                result = (T) new RoleDerivedSchema();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipDerivedSchema();
                break;
        }

        return result;
    }

    public <T extends AbstractAttribute> Class<T> getAttributeClass() {
        Class result = null;

        switch (this) {
            case USER:
                result = UserAttribute.class;
                break;
            case ROLE:
                result = RoleAttribute.class;
                break;
            case MEMBERSHIP:
                result = MembershipAttribute.class;
                break;
        }

        return result;
    }

    public <T extends AbstractAttribute> T newAttribute() {
        T result = null;

        switch (this) {
            case USER:
                result = (T) new UserAttribute();
                break;
            case ROLE:
                result = (T) new RoleAttribute();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipAttribute();
                break;
        }

        return result;
    }

    public <T extends AbstractDerivedAttribute> Class<T> getDerivedAttributeClass() {
        Class result = null;

        switch (this) {
            case USER:
                result = UserDerivedAttribute.class;
                break;
            case ROLE:
                result = RoleDerivedAttribute.class;
                break;
            case MEMBERSHIP:
                result = MembershipDerivedAttribute.class;
                break;
        }

        return result;
    }

    public <T extends AbstractDerivedAttribute> T newDerivedAttribute() {
        T result = null;

        switch (this) {
            case USER:
                result = (T) new UserDerivedAttribute();
                break;
            case ROLE:
                result = (T) new RoleDerivedAttribute();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipDerivedAttribute();
                break;
        }

        return result;
    }

    public <T extends AbstractAttributeValue> Class<T> getAttributeValueClass() {
        Class result = null;

        switch (this) {
            case USER:
                result = UserAttributeValue.class;
                break;
            case ROLE:
                result = RoleAttributeValue.class;
                break;
            case MEMBERSHIP:
                result = MembershipAttributeValue.class;
                break;
        }

        return result;
    }

    public <T extends AbstractAttributeValue> T newAttributeValue() {
        T result = null;

        switch (this) {
            case USER:
                result = (T) new UserAttributeValue();
                break;
            case ROLE:
                result = (T) new RoleAttributeValue();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipAttributeValue();
                break;
        }

        return result;
    }
}
