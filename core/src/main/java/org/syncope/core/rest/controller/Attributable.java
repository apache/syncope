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
package org.syncope.core.rest.controller;

import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.role.RoleDerivedSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;

enum Attributable {

    USER, ROLE;

    public <T extends AbstractSchema> Class<T> getSchemaClass() {
        Class result = null;

        switch (this) {
            case USER:
                result = UserSchema.class;
                break;
            case ROLE:
                result = RoleSchema.class;
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
        }

        return result;
    }
}
