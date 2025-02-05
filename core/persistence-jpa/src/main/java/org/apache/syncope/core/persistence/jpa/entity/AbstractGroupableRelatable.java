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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.GroupablePlainAttr;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.Relationship;

public abstract class AbstractGroupableRelatable<
        L extends Any<P>, 
        M extends Membership<L>, 
        P extends GroupablePlainAttr<L, M>,
        R extends Any<?>,
        REL extends Relationship<L, R>>
        extends AbstractRelatable<L, P, R, REL> implements Groupable<L, M, P, R, REL> {

    private static final long serialVersionUID = -2269285197388729673L;

    @Override
    public List<? extends P> getPlainAttrs() {
        return getPlainAttrsList().stream().
                filter(attr -> attr.getMembershipKey() == null).
                toList();
    }

    @Override
    public Optional<? extends P> getPlainAttr(final String plainSchema) {
        return getPlainAttrsList().stream().
                filter(attr -> attr.getMembershipKey() == null
                && plainSchema.equals(attr.getSchemaKey())).
                findFirst();
    }

    @Override
    public Optional<? extends P> getPlainAttr(final String plainSchema, final Membership<?> membership) {
        return getPlainAttrsList().stream().
                filter(attr -> plainSchema.equals(attr.getSchemaKey())
                && membership.getKey().equals(attr.getMembershipKey())).
                findFirst();
    }

    @Override
    public Collection<? extends P> getPlainAttrs(final String plainSchema) {
        return getPlainAttrsList().stream().
                filter(attr -> plainSchema.equals(attr.getSchemaKey())).
                toList();
    }

    @Override
    public Collection<? extends P> getPlainAttrs(final Membership<?> membership) {
        return getPlainAttrsList().stream().
                filter(attr -> membership.getKey().equals(attr.getMembershipKey())).
                toList();
    }

    @Override
    public Optional<? extends M> getMembership(final String groupKey) {
        return getMemberships().stream().
                filter(membership -> groupKey.equals(membership.getRightEnd().getKey())).
                findFirst();
    }
}
