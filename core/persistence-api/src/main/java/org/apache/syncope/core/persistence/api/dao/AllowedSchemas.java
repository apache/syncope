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
import java.util.Set;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.group.Group;

public class AllowedSchemas<S extends Schema> {

    private final Set<S> forSelf = new HashSet<>();

    private final Map<Group, Set<S>> forMemberships = new HashMap<>();

    public Set<S> getForSelf() {
        return forSelf;
    }

    public Set<S> getForMembership(final Group group) {
        return SetUtils.emptyIfNull(forMemberships.get(group));
    }

    public Map<Group, Set<S>> getForMemberships() {
        return forMemberships;
    }

    public boolean forSelfContains(final S schema) {
        return forSelf.contains(schema);
    }

    public boolean forSelfContains(final String schema) {
        return IterableUtils.matchesAny(forSelf, new KeyMatches(schema));
    }

    public boolean forMembershipsContains(final Group group, final S schema) {
        return IterableUtils.matchesAny(forMemberships.get(group), PredicateUtils.equalPredicate(schema));
    }

    public boolean forMembershipsContains(final S schema) {
        for (Map.Entry<Group, Set<S>> entry : forMemberships.entrySet()) {
            if (entry.getValue().contains(schema)) {
                return true;
            }
        }
        return false;
    }

    public boolean forMembershipsContains(final Group group, final String schema) {
        return IterableUtils.matchesAny(forMemberships.get(group), new KeyMatches(schema));
    }

    public boolean forMembershipsContains(final String schema) {
        KeyMatches keyMatches = new KeyMatches(schema);

        for (Map.Entry<Group, Set<S>> entry : forMemberships.entrySet()) {
            if (IterableUtils.matchesAny(entry.getValue(), keyMatches)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(final S schema) {
        if (forSelfContains(schema)) {
            return true;
        }
        return forMembershipsContains(schema);
    }

    public boolean contains(final String schema) {
        if (forSelfContains(schema)) {
            return true;
        }
        return forMembershipsContains(schema);
    }

    private class KeyMatches implements Predicate<S> {

        private final String schema;

        KeyMatches(final String schema) {
            this.schema = schema;
        }

        @Override
        public boolean evaluate(final S object) {
            return object.getKey().equals(schema);
        }

    }
}
