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

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.springframework.data.neo4j.core.schema.PostLoad;

public abstract class AbstractRelatable<
        L extends Any,
        R extends Relationship<L, AnyObject>>
        extends AbstractAny implements Relatable<L, R> {

    private static final long serialVersionUID = -2269285197388729673L;

    protected abstract List<? extends AbstractRelationship<L, AnyObject>> relationships();

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema, final Relationship<?, ?> relationship) {
        return relationships().stream().
                filter(r -> r.getKey().equals(relationship.getKey())).findFirst().
                flatMap(m -> m.getPlainAttr(plainSchema));
    }

    @Override
    public List<PlainAttr> getPlainAttrs(final Relationship<?, ?> relationship) {
        return relationships().stream().
                filter(r -> r.getKey().equals(relationship.getKey())).
                flatMap(m -> m.getPlainAttrs().stream()).toList();
    }

    @Override
    public Optional<? extends R> getRelationship(
            final RelationshipType relationshipType, final String otherEndKey) {

        return getRelationships().stream().filter(relationship -> relationshipType.equals(relationship.getType())
                && otherEndKey != null
                && (otherEndKey.equals(relationship.getLeftEnd().getKey())
                || otherEndKey.equals(relationship.getRightEnd().getKey()))).findFirst();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<? extends R> getRelationships() {
        return relationships().stream().map(r -> (R) r).toList();
    }

    @Override
    public List<? extends R> getRelationships(final RelationshipType relationshipType) {
        return getRelationships().stream().
                filter(relationship -> relationshipType.equals(relationship.getType())).
                toList();
    }

    @Override
    public List<? extends R> getRelationships(final String otherEndKey) {
        return getRelationships().stream().
                filter(relationship -> otherEndKey.equals(relationship.getRightEnd().getKey())).
                toList();
    }

    @PostLoad
    public void completeRelationshipPlainAttrs() {
        relationships().forEach(m -> doComplete(m.plainAttrs()));
    }
}
