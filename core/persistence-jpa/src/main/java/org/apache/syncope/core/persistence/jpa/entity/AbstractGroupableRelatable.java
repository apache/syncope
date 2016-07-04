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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.GroupablePlainAttr;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.GroupableRelatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;

public abstract class AbstractGroupableRelatable<
        L extends Any<P>, 
        M extends Membership<L>, 
        P extends GroupablePlainAttr<L, M>,
        R extends Any<?>,
        REL extends Relationship<L, R>>
        extends AbstractAny<P> implements GroupableRelatable<L, M, P, R, REL> {

    private static final long serialVersionUID = -2269285197388729673L;

    protected abstract List<? extends P> internalGetPlainAttrs();

    @Override
    public boolean remove(final P attr) {
        return internalGetPlainAttrs().remove(attr);
    }

    @Override
    public P getPlainAttr(final String plainSchemaName) {
        return IterableUtils.find(internalGetPlainAttrs(), new Predicate<P>() {

            @Override
            public boolean evaluate(final P plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && plainAttr.getMembership() == null
                        && plainSchemaName.equals(plainAttr.getSchema().getKey());
            }
        });
    }

    @Override
    public P getPlainAttr(final String plainSchemaName, final Membership<?> membership) {
        return IterableUtils.find(internalGetPlainAttrs(), new Predicate<P>() {

            @Override
            public boolean evaluate(final P plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && plainAttr.getMembership() != null && plainAttr.getMembership().equals(membership)
                        && plainSchemaName.equals(plainAttr.getSchema().getKey());
            }
        });
    }

    @Override
    public List<? extends P> getPlainAttrs() {
        return CollectionUtils.select(internalGetPlainAttrs(), new Predicate<P>() {

            @Override
            public boolean evaluate(final P plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && plainAttr.getMembership() == null;
            }
        }, new ArrayList<P>());
    }

    @Override
    public Collection<? extends P> getPlainAttrs(final String plainSchemaName) {
        return CollectionUtils.select(internalGetPlainAttrs(), new Predicate<P>() {

            @Override
            public boolean evaluate(final P plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && plainSchemaName.equals(plainAttr.getSchema().getKey());
            }
        });
    }

    @Override
    public Collection<? extends P> getPlainAttrs(final Membership<?> membership) {
        return CollectionUtils.select(internalGetPlainAttrs(), new Predicate<P>() {

            @Override
            public boolean evaluate(final P plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && membership.equals(plainAttr.getMembership());
            }
        });
    }

    @Override
    public M getMembership(final String groupKey) {
        return IterableUtils.find(getMemberships(), new Predicate<M>() {

            @Override
            public boolean evaluate(final M membership) {
                return groupKey != null && groupKey.equals(membership.getRightEnd().getKey());
            }
        });
    }

    @Override
    public REL getRelationship(final RelationshipType relationshipType, final String otherEndKey) {
        return IterableUtils.find(getRelationships(), new Predicate<REL>() {

            @Override
            public boolean evaluate(final REL relationship) {
                return otherEndKey != null && otherEndKey.equals(relationship.getRightEnd().getKey())
                        && ((relationshipType == null && relationship.getType() == null)
                        || (relationshipType != null && relationshipType.equals(relationship.getType())));
            }
        });
    }

    @Override
    public Collection<? extends REL> getRelationships(final RelationshipType relationshipType) {
        return CollectionUtils.select(getRelationships(), new Predicate<REL>() {

            @Override
            public boolean evaluate(final REL relationship) {
                return relationshipType != null && relationshipType.equals(relationship.getType());
            }
        });
    }

    @Override
    public Collection<? extends REL> getRelationships(final String otherEndKey) {
        return CollectionUtils.select(getRelationships(), new Predicate<REL>() {

            @Override
            public boolean evaluate(final REL relationship) {
                return otherEndKey != null && otherEndKey.equals(relationship.getRightEnd().getKey());
            }
        });
    }
}
