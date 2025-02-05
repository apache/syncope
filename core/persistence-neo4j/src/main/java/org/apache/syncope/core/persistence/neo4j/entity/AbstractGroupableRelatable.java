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
package org.apache.syncope.core.persistence.neo4j.entity;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.GroupablePlainAttr;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.springframework.data.neo4j.core.schema.PostLoad;

public abstract class AbstractGroupableRelatable<
        L extends Any<P>,
        M extends Membership<L>,
        P extends GroupablePlainAttr<L, M>,
        R extends Any<?>,
        REL extends Relationship<L, R>>
        extends AbstractRelatable<L, P, R, REL> implements Groupable<L, M, P, R, REL> {

    private static final long serialVersionUID = -2269285197388729673L;

    protected abstract List<? extends AbstractMembership<L, P>> memberships();

    @Override
    public boolean remove(final P attr) {
        if (attr.getMembershipKey() == null) {
            return plainAttrs().put(attr.getSchemaKey(), null) != null;
        }

        return memberships().stream().
                filter(m -> m.getKey().equals(attr.getMembershipKey())).findFirst().
                map(membership -> membership.plainAttrs().put(attr.getSchemaKey(), null) != null).
                orElse(false);
    }

    @Override
    public Optional<? extends P> getPlainAttr(final String plainSchema, final Membership<?> membership) {
        return memberships().stream().
                filter(m -> m.getKey().equals(membership.getKey())).findFirst().
                flatMap(m -> m.getPlainAttr(plainSchema));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends P> getPlainAttrs() {
        return plainAttrs().entrySet().stream().
                filter(e -> e.getValue() != null).
                sorted(Comparator.comparing(Map.Entry::getKey)).
                map(e -> (P) e.getValue()).toList();
    }

    @Override
    public Collection<? extends P> getPlainAttrs(final String plainSchema) {
        return Stream.concat(getPlainAttr(plainSchema).map(Stream::of).orElse(Stream.empty()),
                memberships().stream().map(m -> m.getPlainAttr(plainSchema)).
                        flatMap(Optional::stream)).
                toList();
    }

    @Override
    public Collection<? extends P> getPlainAttrs(final Membership<?> membership) {
        return memberships().stream().
                filter(m -> m.getKey().equals(membership.getKey())).
                flatMap(m -> m.getPlainAttrs().stream()).toList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends M> getMemberships() {
        return memberships().stream().map(m -> (M) m).toList();
    }

    @Override
    public Optional<? extends M> getMembership(final String groupKey) {
        return getMemberships().stream().
                filter(membership -> groupKey != null && groupKey.equals(membership.getRightEnd().getKey())).
                findFirst();
    }

    @PostLoad
    public void completeMembershipPlainAttrs() {
        memberships().forEach(m -> doComplete(m.plainAttrs()));
    }
}
