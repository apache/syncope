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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
import org.apache.syncope.core.persistence.jpa.entity.JPARelationshipType;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;

public class RelationshipTypeRepoExtImpl implements RelationshipTypeRepoExt {

    protected final EntityManager entityManager;

    public RelationshipTypeRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<String> findByEndAnyType(final AnyType anyType) {
        TypedQuery<RelationshipType> query = entityManager.createQuery(
                "SELECT DISTINCT e FROM " + JPARelationshipType.class.getSimpleName() + " e "
                + "WHERE e.leftEndAnyType=:anyType OR e.rightEndAnyType=:anyType",
                RelationshipType.class);
        query.setParameter("anyType", anyType);
        return query.getResultList().stream().map(RelationshipType::getKey).toList();
    }

    @Override
    public List<? extends RelationshipType> findByLeftEndAnyType(final AnyType anyType) {
        TypedQuery<RelationshipType> query = entityManager.createQuery(
                "SELECT DISTINCT e FROM " + JPARelationshipType.class.getSimpleName() + " e "
                + "WHERE e.leftEndAnyType=:anyType",
                RelationshipType.class);
        query.setParameter("anyType", anyType);
        return query.getResultList();
    }

    protected Collection<? extends Relationship<?, ?>> findRelationshipsByType(final RelationshipType type) {
        TypedQuery<ARelationship> aquery = entityManager.createQuery(
                "SELECT e FROM " + JPAARelationship.class.getSimpleName() + " e WHERE e.type=:type",
                ARelationship.class);
        aquery.setParameter("type", type);
        TypedQuery<URelationship> uquery = entityManager.createQuery(
                "SELECT e FROM " + JPAURelationship.class.getSimpleName() + " e WHERE e.type=:type",
                URelationship.class);
        uquery.setParameter("type", type);

        List<Relationship<?, ?>> result = new ArrayList<>();
        result.addAll(aquery.getResultList());
        result.addAll(uquery.getResultList());

        return result;
    }

    @Override
    public void deleteById(final String key) {
        RelationshipType type = entityManager.find(JPARelationshipType.class, key);
        if (type == null) {
            return;
        }

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
        }).forEach(entityManager::remove);

        entityManager.remove(type);
    }
}
