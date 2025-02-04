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
package org.apache.syncope.common.lib.search;

import java.util.Arrays;
import java.util.Optional;

public enum SpecialAttr {

    /**
     * Applies to users, groups and any objects.
     */
    NULL("$null"),
    /**
     * Applies to any objects.
     */
    TYPE("$type"),
    /**
     * Applies to users, groups and any objects.
     */
    AUX_CLASSES("$auxClasses"),
    /**
     * Applies to users, groups and any objects.
     */
    RESOURCES("$resources"),
    /**
     * Applies to users and any objects.
     */
    GROUPS("$groups"),
    /**
     * Applies to users and any objects.
     */
    RELATIONSHIPS("$relationships"),
    /**
     * Applies to users and any objects.
     */
    RELATIONSHIP_TYPES("$relationshipTypes"),
    /**
     * Applies to users.
     */
    ROLES("$roles"),
    /**
     * Applies to users, groups and any objects.
     */
    DYNREALMS("$dynRealms"),
    /**
     * Applies to groups.
     */
    MEMBER("$member");

    private final String literal;

    SpecialAttr(final String literal) {
        this.literal = literal;
    }

    @Override
    public String toString() {
        return literal;
    }

    public static Optional<SpecialAttr> fromString(final String value) {
        return Arrays.stream(values()).filter(specialAttr -> specialAttr.literal.equals(value)).findFirst();
    }
}
