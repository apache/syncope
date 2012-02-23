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
package org.syncope.types;

import java.util.EnumSet;

/**
 * Internal attribute mapping type.
 */
public enum IntMappingType {

    // Unfortunately enum type cannot be extended ...
    // -------------------------
    // User attribute types (the same in UserMappingType)
    // -------------------------
    UserSchema(Entity.USER),
    UserDerivedSchema(Entity.USER),
    UserVirtualSchema(Entity.USER),
    SyncopeUserId(Entity.USER),
    Password(Entity.USER),
    Username(Entity.USER),
    // -------------------------
    // Role attribute types (the same in RoleMappingType)
    // -------------------------
    RoleSchema(Entity.ROLE),
    RoleDerivedSchema(Entity.ROLE),
    RoleVirtualSchema(Entity.ROLE),
    // -------------------------
    // Membership attribute types (the same in MembershipMappingType)
    // -------------------------
    MembershipSchema(Entity.MEMBERSHIP),
    MembershipDerivedSchema(Entity.MEMBERSHIP),
    MembershipVirtualSchema(Entity.MEMBERSHIP);

    private Entity entity;

    private IntMappingType(final Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    /**
     * Get attribute types for a certain entity.
     *
     * @param entity entity.
     * @return set of attribute types.
     */
    public static EnumSet getAttributeTypes(final Entity entity) {
        switch (entity) {
            case ROLE:
                return EnumSet.allOf(RoleMappingType.class);
            case MEMBERSHIP:
                return EnumSet.allOf(MembershipMappingType.class);
            default:
                return EnumSet.allOf(UserMappingType.class);
        }
    }

    /**
     * Check if attribute type belongs to the specified entity set.
     *
     * @param entity entity.
     * @param type attrybute type.
     * @return true if attribute type belongs to the specified entity set.
     */
    public static boolean contains(
            final Entity entity, final String type) {

        switch (entity) {
            case ROLE:
                return RoleMappingType.valueOf(type) != null;
            case MEMBERSHIP:
                return MembershipMappingType.valueOf(type) != null;
            default:
                return UserMappingType.valueOf(type) != null;
        }
    }

    /**
     * User attribute types.
     */
    enum UserMappingType {

        UserSchema,
        UserDerivedSchema,
        UserVirtualSchema,
        SyncopeUserId,
        Password,
        Username;
    }

    /**
     * Role attribute types.
     */
    private enum RoleMappingType {

        RoleSchema,
        RoleDerivedSchema,
        RoleVirtualSchema;
    }

    /**
     * Membership attribute types.
     */
    private enum MembershipMappingType {

        MembershipSchema,
        MembershipDerivedSchema,
        MembershipVirtualSchema;
    }
}
