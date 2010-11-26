package org.syncope.types;

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
public enum SchemaType {

    UserSchema(
    "org.syncope.core.persistence.beans.user.USchema"),
    RoleSchema(
    "org.syncope.core.persistence.beans.role.RSchema"),
    MembershipSchema(
    "org.syncope.core.persistence.beans.membership.MSchema"),
    AccountId(
    "AccountId"),
    Password(
    "Password");

    final private String className;

    SchemaType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Class getSchemaClass() {
        try {
            return Class.forName(getClassName());
        } catch (ClassNotFoundException e) {
            return String.class;
        }
    }

    public static SchemaType byClass(Class theClass) {
        for (SchemaType schemaType : SchemaType.values()) {
            if (schemaType.getClassName().equals(theClass.getName())) {
                return schemaType;
            }
        }

        return null;
    }
}
