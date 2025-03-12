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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRelationshipType;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jARelationship;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class RelationshipTypeRepoExtImpl extends AbstractDAO implements RelationshipTypeRepoExt {

    public RelationshipTypeRepoExtImpl(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        super(neo4jTemplate, neo4jClient);
    }

    @Override
    public List<String> findByEndAnyType(final AnyType anyType) {
        List<Neo4jRelationshipType> result = findByRelationship(
                Neo4jRelationshipType.NODE, Neo4jAnyType.NODE, anyType.getKey(), Neo4jRelationshipType.class, null);
        return result.stream().map(RelationshipType::getKey).distinct().toList();
    }

    @Override
    public List<? extends RelationshipType> findByLeftEndAnyType(final AnyType anyType) {
        return findByRelationship(
                Neo4jRelationshipType.NODE,
                Neo4jAnyType.NODE,
                anyType.getKey(),
                Neo4jRelationshipType.LEFT_END_ANYTYPE,
                Neo4jRelationshipType.class,
                null);
    }

    protected Collection<? extends Relationship<?, ?>> findRelationshipsByType(final RelationshipType type) {
        List<Relationship<?, ?>> result = new ArrayList<>();

        result.addAll(findByRelationship(
                Neo4jURelationship.NODE, Neo4jRelationshipType.NODE, type.getKey(), Neo4jURelationship.class, null));
        result.addAll(findByRelationship(
                Neo4jARelationship.NODE, Neo4jRelationshipType.NODE, type.getKey(), Neo4jARelationship.class, null));

        return result;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jRelationshipType.class).ifPresent(type -> {
            findRelationshipsByType(type).stream().peek(relationship -> {
                switch (relationship) {
                    case URelationship uRelationship ->
                        uRelationship.getLeftEnd().getRelationships().remove(uRelationship);
                    case UMembership uMembership ->
                        uMembership.getLeftEnd().remove(uMembership);
                    case ARelationship aRelationship ->
                        aRelationship.getLeftEnd().getRelationships().remove(aRelationship);
                    case AMembership aMembership ->
                        aMembership.getLeftEnd().remove(aMembership);
                    default -> {
                    }
                }
                relationship.setLeftEnd(null);
            }).forEach(r -> neo4jTemplate.deleteById(r.getKey(), r.getClass()));

            neo4jTemplate.deleteById(key, Neo4jRelationshipType.class);
        });
    }
}
