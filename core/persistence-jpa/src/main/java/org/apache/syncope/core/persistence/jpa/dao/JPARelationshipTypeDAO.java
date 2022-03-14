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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.jpa.entity.JPARelationshipType;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;

public class JPARelationshipTypeDAO extends AbstractDAO<RelationshipType> implements RelationshipTypeDAO {

    @Override
    public RelationshipType find(final String key) {
        return entityManager().find(JPARelationshipType.class, key);
    }

    @Override
    public List<RelationshipType> findAll() {
        TypedQuery<RelationshipType> query = entityManager().createQuery(
                "SELECT e FROM " + JPARelationshipType.class.getSimpleName() + " e ", RelationshipType.class);
        return query.getResultList();
    }

    @Override
    public RelationshipType save(final RelationshipType anyType) {
        return entityManager().merge(anyType);
    }

    private Collection<? extends Relationship<?, ?>> findRelationshipsByType(final RelationshipType type) {
        TypedQuery<ARelationship> aquery = entityManager().createQuery(
                "SELECT e FROM " + JPAARelationship.class.getSimpleName() + " e WHERE e.type=:type",
                ARelationship.class);
        aquery.setParameter("type", type);
        TypedQuery<URelationship> uquery = entityManager().createQuery(
                "SELECT e FROM " + JPAURelationship.class.getSimpleName() + " e WHERE e.type=:type",
                URelationship.class);
        uquery.setParameter("type", type);

        List<Relationship<?, ?>> result = new ArrayList<>();
        result.addAll(aquery.getResultList());
        result.addAll(uquery.getResultList());

        return result;
    }

    @Override
    public void delete(final String key) {
        RelationshipType type = find(key);
        if (type == null) {
            return;
        }

        findRelationshipsByType(type).stream().map(relationship -> {
            if (relationship instanceof URelationship) {
                ((URelationship) relationship).getLeftEnd().getRelationships().remove((URelationship) relationship);
            } else if (relationship instanceof UMembership) {
                ((UMembership) relationship).getLeftEnd().remove((UMembership) relationship);
            } else if (relationship instanceof ARelationship) {
                ((ARelationship) relationship).getLeftEnd().getRelationships().remove((ARelationship) relationship);
            } else if (relationship instanceof AMembership) {
                ((AMembership) relationship).getLeftEnd().remove((AMembership) relationship);
            }
            relationship.setLeftEnd(null);
            return relationship;
        }).forEach(entityManager()::remove);

        entityManager().remove(type);
    }
}
