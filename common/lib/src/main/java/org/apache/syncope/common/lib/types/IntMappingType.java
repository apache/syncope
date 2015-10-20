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
package org.apache.syncope.common.lib.types;

import java.util.Collection;
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
    UserPlainSchema(AnyTypeKind.USER),
    UserDerivedSchema(AnyTypeKind.USER),
    UserVirtualSchema(AnyTypeKind.USER),
    UserKey(AnyTypeKind.USER),
    Username(AnyTypeKind.USER),
    Password(AnyTypeKind.USER),
    // -------------------------
    // Group attribute types (the same in GroupMappingType)
    // -------------------------
    GroupPlainSchema(AnyTypeKind.GROUP),
    GroupDerivedSchema(AnyTypeKind.GROUP),
    GroupVirtualSchema(AnyTypeKind.GROUP),
    GroupKey(AnyTypeKind.GROUP),
    GroupName(AnyTypeKind.GROUP),
    GroupOwnerSchema(AnyTypeKind.GROUP),
    // -------------------------
    // Any attribute types (the same in AnyMappingType)
    // -------------------------
    AnyObjectPlainSchema(AnyTypeKind.ANY_OBJECT),
    AnyObjectDerivedSchema(AnyTypeKind.ANY_OBJECT),
    AnyObjectVirtualSchema(AnyTypeKind.ANY_OBJECT),
    AnyObjectKey(AnyTypeKind.ANY_OBJECT);

    private final AnyTypeKind anyTypeKind;

    IntMappingType(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    /**
     * Get attribute types for a certain any object type.
     *
     * @param anyTypeKind any object type
     * @param toBeFiltered types to be filtered from the result.
     * @return set of attribute types.
     */
    public static Set<IntMappingType> getAttributeTypes(
            final AnyTypeKind anyTypeKind, final Collection<IntMappingType> toBeFiltered) {

        final Set<IntMappingType> res = getAttributeTypes(anyTypeKind);
        res.removeAll(toBeFiltered);

        return res;
    }

    /**
     * Get attribute types for a certain any object type.
     *
     * @param anyTypeKind any object type
     * @return set of attribute types.
     */
    public static Set<IntMappingType> getAttributeTypes(final AnyTypeKind anyTypeKind) {
        EnumSet<?> enumset;

        switch (anyTypeKind) {
            case GROUP:
                enumset = EnumSet.allOf(GroupMappingType.class);
                break;

            case ANY_OBJECT:
                enumset = EnumSet.allOf(AnyMappingType.class);
                break;

            case USER:
            default:
                enumset = EnumSet.allOf(UserMappingType.class);
                break;
        }

        final Set<IntMappingType> result = new HashSet<>(enumset.size());
        for (Object obj : enumset) {
            result.add(IntMappingType.valueOf(obj.toString()));
        }

        return result;
    }

    public static Set<IntMappingType> getEmbedded() {
        return EnumSet.of(IntMappingType.UserKey, IntMappingType.Username, IntMappingType.Password,
                IntMappingType.GroupKey, IntMappingType.GroupName, IntMappingType.GroupOwnerSchema,
                IntMappingType.AnyObjectKey);
    }

    /**
     * Check if attribute type belongs to the specified any object type set.
     *
     * @param anyTypeKind any object type.
     * @param type attribute type.
     * @return true if attribute type belongs to the specified any object type set.
     */
    public static boolean contains(final AnyTypeKind anyTypeKind, final String type) {
        switch (anyTypeKind) {
            case GROUP:
                for (GroupMappingType c : GroupMappingType.values()) {
                    if (c.name().equals(type)) {
                        return true;
                    }
                }
                break;

            case ANY_OBJECT:
                for (AnyMappingType c : AnyMappingType.values()) {
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

        UserPlainSchema,
        UserDerivedSchema,
        UserVirtualSchema,
        UserKey,
        Username,
        Password;

    }

    /**
     * Group attribute types.
     */
    private enum GroupMappingType {

        GroupPlainSchema,
        GroupDerivedSchema,
        GroupVirtualSchema,
        GroupKey,
        GroupName,
        GroupOwnerSchema;

    }

    /**
     * Any attribute types.
     */
    private enum AnyMappingType {

        AnyObjectPlainSchema,
        AnyObjectDerivedSchema,
        AnyObjectVirtualSchema,
        AnyObjectKey;

    }
}
