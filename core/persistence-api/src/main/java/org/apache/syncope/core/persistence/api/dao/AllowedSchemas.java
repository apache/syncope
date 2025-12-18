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
package org.apache.syncope.core.persistence.api.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.group.Group;

public class AllowedSchemas<S extends Schema> {

    private final Set<S> self = new HashSet<>();

    private final Map<Group, Set<S>> memberships = new HashMap<>();

    private final Map<RelationshipType, Set<S>> relationshipTypes = new HashMap<>();

    public Set<S> self() {
        return self;
    }

    public Set<S> membership(final Group group) {
        return Optional.ofNullable(memberships.get(group)).orElseGet(Set::of);
    }

    public Map<Group, Set<S>> memberships() {
        return memberships;
    }

    public Set<S> relationshipType(final RelationshipType relationshipType) {
        return Optional.ofNullable(relationshipTypes.get(relationshipType)).orElseGet(Set::of);
    }

    public Map<RelationshipType, Set<S>> relationshipTypes() {
        return relationshipTypes;
    }

    public boolean selfContains(final S schema) {
        return self.contains(schema);
    }

    public boolean selfContains(final String schema) {
        return self.stream().anyMatch(s -> s.getKey().equals(schema));
    }

    public boolean membershipsContains(final Group group, final S schema) {
        return membership(group).contains(schema);
    }

    public boolean membershipsContains(final Group group, final String schema) {
        return membership(group).stream().anyMatch(s -> s.getKey().equals(schema));
    }

    public boolean relationshipTypesContains(final RelationshipType relationshipType, final S schema) {
        return relationshipType(relationshipType).contains(schema);
    }

    public boolean relationshipTypesContains(final RelationshipType relationshipType, final String schema) {
        return relationshipType(relationshipType).stream().anyMatch(s -> s.getKey().equals(schema));
    }
}
