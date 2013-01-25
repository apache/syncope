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
package org.apache.syncope.common.types;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Internal attribute mapping type.
 */
@XmlEnum
public enum IntMappingType {

    // Unfortunately enum type cannot be extended ...
    // -------------------------
    // User attribute types (the same in UserMappingType)
    // -------------------------
    UserSchema(AttributableType.USER),
    UserDerivedSchema(AttributableType.USER),
    UserVirtualSchema(AttributableType.USER),
    UserId(AttributableType.USER),
    Username(AttributableType.USER),
    Password(AttributableType.USER),
    // -------------------------
    // Role attribute types (the same in RoleMappingType)
    // -------------------------
    RoleSchema(AttributableType.ROLE),
    RoleDerivedSchema(AttributableType.ROLE),
    RoleVirtualSchema(AttributableType.ROLE),
    RoleId(AttributableType.ROLE),
    RoleName(AttributableType.ROLE),
    RoleOwnerSchema(AttributableType.ROLE),
    // -------------------------
    // Membership attribute types (the same in MembershipMappingType)
    // -------------------------
    MembershipSchema(AttributableType.MEMBERSHIP),
    MembershipDerivedSchema(AttributableType.MEMBERSHIP),
    MembershipVirtualSchema(AttributableType.MEMBERSHIP),
    MembershipId(AttributableType.MEMBERSHIP);

    private AttributableType attributableType;

    private IntMappingType(final AttributableType attributableType) {
        this.attributableType = attributableType;
    }

    public AttributableType getAttributableType() {
        return attributableType;
    }

    /**
     * Get attribute types for a certain attributable type.
     *
     * @param attributableType attributable type
     * @param toBeFiltered types to be filtered from the result.
     * @return set of attribute types.
     */
    public static Set<IntMappingType> getAttributeTypes(
            final AttributableType attributableType, final Set<IntMappingType> toBeFiltered) {

        final Set<IntMappingType> res = getAttributeTypes(attributableType);
        res.removeAll(toBeFiltered);

        return res;
    }

    /**
     * Get attribute types for a certain attributable type.
     *
     * @param attributableType attributable type
     * @return set of attribute types.
     */
    public static Set<IntMappingType> getAttributeTypes(final AttributableType attributableType) {
        final EnumSet<?> enumset;

        switch (attributableType) {
            case ROLE:
                enumset = EnumSet.allOf(RoleMappingType.class);
                break;

            case MEMBERSHIP:
                enumset = EnumSet.allOf(MembershipMappingType.class);
                break;

            default:
                enumset = EnumSet.allOf(UserMappingType.class);
        }

        final Set<IntMappingType> result = new HashSet<IntMappingType>(enumset.size());
        for (Object obj : enumset) {
            result.add(IntMappingType.valueOf(obj.toString()));
        }

        return result;
    }

    public static Set<IntMappingType> getEmbedded() {
        return EnumSet.of(
                IntMappingType.UserId, IntMappingType.Username, IntMappingType.Password,
                IntMappingType.RoleId, IntMappingType.RoleName,
                IntMappingType.MembershipId);
    }

    /**
     * Check if attribute type belongs to the specified attributable type set.
     *
     * @param attributableType attributable type.
     * @param type attribute type.
     * @return true if attribute type belongs to the specified attributable type set.
     */
    public static boolean contains(final AttributableType attributableType, final String type) {
        switch (attributableType) {
            case ROLE:
                for (RoleMappingType c : RoleMappingType.values()) {
                    if (c.name().equals(type)) {
                        return true;
                    }
                }
                break;

            case MEMBERSHIP:
                for (MembershipMappingType c : MembershipMappingType.values()) {
                    if (c.name().equals(type)) {
                        return true;
                    }
                }
                break;

            case USER:
            default:
                for (UserMappingType c : UserMappingType.values()) {
                    if (c.name().equals(type)) {
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    /**
     * User attribute types.
     */
    private enum UserMappingType {

        UserSchema,
        UserDerivedSchema,
        UserVirtualSchema,
        UserId,
        Username,
        Password;

    }

    /**
     * Role attribute types.
     */
    private enum RoleMappingType {

        RoleSchema,
        RoleDerivedSchema,
        RoleVirtualSchema,
        RoleId,
        RoleName,
        RoleOwnerSchema;

    }

    /**
     * Membership attribute types.
     */
    private enum MembershipMappingType {

        MembershipSchema,
        MembershipDerivedSchema,
        MembershipVirtualSchema,
        MembershipId;

    }
}
